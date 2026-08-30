/**
 * CX Tracker — Cloud Functions
 *
 * Deploy with: firebase deploy --only functions
 *
 * Required one-time setup (see ../SETUP_FIREBASE.md):
 *   firebase functions:config:set smtp.user="you@gmail.com" smtp.pass="your-app-password"
 * or, if using the 2nd-gen functions + Secret Manager, set SMTP_USER / SMTP_PASS
 * as environment secrets from the Firebase Console.
 */

const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();
setGlobalOptions({ maxInstances: 10 });

const db = admin.firestore();

// ---------------------------------------------------------------------------
// 1) Keep users_by_uid/{authUid} in sync with users/{id}, server-side only.
//    Firestore Security Rules trust this mirror to decide "is this caller a
//    Unit Head" — it must never be writable by the client itself (see
//    firestore.rules), otherwise anyone could fake their own admin mirror.
// ---------------------------------------------------------------------------
exports.onUserProfileWritten = onDocumentWritten("users/{docId}", async (event) => {
  const after = event.data?.after?.data();
  const before = event.data?.before?.data();

  // Document deleted
  if (!after) {
    const uid = before?.authUid;
    if (uid) {
      await db.collection("users_by_uid").doc(uid).delete().catch(() => {});
    }
    return;
  }

  if (!after.authUid) return; // profile not yet linked to a Firebase Auth account

  await db.collection("users_by_uid").doc(after.authUid).set(after, { merge: false });
});

// ---------------------------------------------------------------------------
// 2) Automatically email the Unit Head whenever a new team member signs up.
//    This replaces the old flow, where the NEW user's own phone had to open
//    a mailto: draft and manually hit send — which is why the admin often
//    never actually found out about new sign-ups.
// ---------------------------------------------------------------------------
function buildTransport() {
  const user = process.env.SMTP_USER;
  const pass = process.env.SMTP_PASS;
  if (!user || !pass) return null;
  return nodemailer.createTransport({
    service: "gmail", // swap for your own SMTP provider if not using Gmail
    auth: { user, pass },
  });
}

exports.onUserCreated = onDocumentWritten("users/{docId}", async (event) => {
  const before = event.data?.before?.data();
  const after = event.data?.after?.data();
  if (before || !after) return; // only fire on brand-new documents, not edits/deletes

  const unitSnap = after.unitId
    ? await db.collection("units").doc(String(after.unitId)).get()
    : null;
  const unit = unitSnap?.exists ? unitSnap.data() : null;
  const headEmail = unit?.headEmail || "sabeen.shafique@example.com";
  const headName = unit?.unitHeadName || "Unit Head";

  const transporter = buildTransport();
  if (!transporter) {
    console.warn("SMTP not configured (SMTP_USER/SMTP_PASS) — skipping onboarding email.");
    return;
  }

  await transporter.sendMail({
    from: `"CX Tracker" <${process.env.SMTP_USER}>`,
    to: headEmail,
    subject: `New CX Tracker sign-up: ${after.fullName}`,
    text:
      `A new team member has joined CX Tracker.\n\n` +
      `Name: ${after.fullName}\n` +
      `Email: ${after.email}\n` +
      `Unit: ${unit?.name || "Unassigned"}\n` +
      `Designation: ${after.designation || ""}\n\n` +
      `— Sent automatically by CX Tracker`,
  });

  console.log(`Onboarding email sent to ${headEmail} for new user ${after.email}`);
});

// ---------------------------------------------------------------------------
// 3) Every 15 minutes, recompute each active task's TAT status server-side,
//    so "breached" is decided the same way for every device (not dependent
//    on one phone's clock, or whether that phone happens to be open).
// ---------------------------------------------------------------------------
exports.recomputeTatStatus = onSchedule("every 15 minutes", async () => {
  const now = Date.now();
  const snap = await db.collection("tasks").where("status", "!=", "COMPLETED").get();

  const batch = db.batch();
  let updates = 0;

  snap.forEach((doc) => {
    const t = doc.data();
    const due = t.dueDateTime;
    const assignedAt = t.assignedAt || t.createdAt || now;
    let status;
    if (now > due) {
      status = "BREACHED_TAT";
    } else {
      const total = Math.max(due - assignedAt, 1);
      const remaining = due - now;
      status = remaining / total <= 0.25 ? "AT_RISK" : "WITHIN_TAT";
    }
    if (t.computedTatStatus !== status) {
      batch.update(doc.ref, { computedTatStatus: status, lastTatCheckAt: now });
      updates++;
    }
  });

  if (updates > 0) await batch.commit();
  console.log(`TAT recompute: checked ${snap.size} active tasks, updated ${updates}.`);
});
