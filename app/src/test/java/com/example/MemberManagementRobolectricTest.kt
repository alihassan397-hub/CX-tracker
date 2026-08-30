package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.dao.CxDao
import com.example.data.db.AppDatabase
import com.example.data.entity.CxUnit
import com.example.data.entity.TeamMember
import com.example.ui.screens.isInternMember
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MemberManagementRobolectricTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: CxDao

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.cxDao()
        // Seed standard initial CX organizational units, members, and user accounts
        AppDatabase.populateInitialHblData(dao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `test Unit Head Sabeen Shafique exists with administrative authority`() = runBlocking {
        val headUser = dao.getUserByEmail("sabeen.shafique@example.com")
        assertNotNull("Unit Head user account should exist", headUser)
        assertEquals("Sabeen Shafique", headUser?.fullName)
        assertTrue("User must possess Unit Head privileges", headUser?.isUnitHead == true)
        assertEquals(com.example.data.entity.UserRole.UNIT_HEAD.name, headUser?.role)
        assertEquals("Customer Experience Unit Head", headUser?.designation)
    }

    @Test
    fun `test add team member mapped to organizational unit`() = runBlocking {
        val newMember = TeamMember(
            id = 200,
            unitId = 1, // Mapped to Service Quality
            fullName = "Taimoor Khan",
            employeeId = "SQ-201",
            email = "taimoor.khan@example.com",
            phone = "+92 300 1234567",
            role = "Team Member",
            designation = "Customer Experience Specialist",
            avatarColorHex = "#008269"
        )
        dao.insertTeamMember(newMember)

        val retrieved = dao.getTeamMemberById(200)
        assertNotNull("New team member must be retrieved from database", retrieved)
        assertEquals("Taimoor Khan", retrieved?.fullName)
        assertEquals("SQ-201", retrieved?.employeeId)
        assertEquals(1L, retrieved?.unitId)
        assertEquals("Customer Experience Specialist", retrieved?.designation)
    }

    @Test
    fun `test add intern and verify unit mapping and isIntern detection`() = runBlocking {
        val internMember = TeamMember(
            id = 201,
            unitId = 5, // Mapped to CX Interns & Trainees unit
            fullName = "Aiman Intern",
            employeeId = "INT-405",
            email = "aiman.intern@example.com",
            phone = "+92 321 7654321",
            role = "Intern",
            designation = "Customer Experience Intern",
            avatarColorHex = "#7C3AED"
        )
        dao.insertTeamMember(internMember)

        val retrievedIntern = dao.getTeamMemberById(201)
        assertNotNull("Intern must be saved in database", retrievedIntern)
        assertEquals("Intern", retrievedIntern?.role)
        assertEquals(5L, retrievedIntern?.unitId)

        val dummyUnits = listOf(
            CxUnit(id = 1, name = "Service Quality", code = "SQ", description = "", unitHeadName = "", headEmail = "", colorHex = "#008269"),
            CxUnit(id = 5, name = "CX Interns & Trainees", code = "INT", description = "", unitHeadName = "", headEmail = "", colorHex = "#7C3AED")
        )
        assertTrue("isInternMember helper must identify intern by role/unit", isInternMember(retrievedIntern!!, dummyUnits))
    }

    @Test
    fun `test reassign member to different organizational unit`() = runBlocking {
        val member = TeamMember(
            id = 202,
            unitId = 1, // Initially Service Quality
            fullName = "Reassign Target",
            employeeId = "CX-202",
            email = "reassign.target@example.com",
            phone = "+92 300 9988776",
            role = "Team Member",
            designation = "Resolution Officer",
            avatarColorHex = "#0284C7"
        )
        dao.insertTeamMember(member)

        val initial = dao.getTeamMemberById(202)
        assertEquals(1L, initial?.unitId)

        // Transfer/Reassign to Complaints Management Unit (unitId = 3)
        val transferred = initial!!.copy(unitId = 3)
        dao.updateTeamMember(transferred)

        val afterTransfer = dao.getTeamMemberById(202)
        assertNotNull(afterTransfer)
        assertEquals(3L, afterTransfer?.unitId)
    }

    @Test
    fun `test edit member profile information`() = runBlocking {
        val member = TeamMember(
            id = 203,
            unitId = 2,
            fullName = "Original Name",
            employeeId = "CXE-203",
            email = "original.name@example.com",
            phone = "+92 333 1112233",
            role = "Team Member",
            designation = "Junior Officer",
            avatarColorHex = "#0284C7"
        )
        dao.insertTeamMember(member)

        val loaded = dao.getTeamMemberById(203)
        assertNotNull(loaded)

        val updated = loaded!!.copy(
            fullName = "Senior Promoted Name",
            designation = "Lead CX Executive",
            email = "promoted.name@example.com",
            phone = "+92 333 9998877"
        )
        dao.updateTeamMember(updated)

        val verified = dao.getTeamMemberById(203)
        assertNotNull(verified)
        assertEquals("Senior Promoted Name", verified?.fullName)
        assertEquals("Lead CX Executive", verified?.designation)
        assertEquals("promoted.name@example.com", verified?.email)
        assertEquals("+92 333 9998877", verified?.phone)
    }

    @Test
    fun `test remove member from team roster`() = runBlocking {
        val member = TeamMember(
            id = 204,
            unitId = 1,
            fullName = "To Be Deleted",
            employeeId = "CX-DEL",
            email = "to.delete@example.com",
            phone = "+92 300 0000000",
            role = "Team Member",
            designation = "Temp Officer",
            avatarColorHex = "#008269"
        )
        dao.insertTeamMember(member)

        val created = dao.getTeamMemberById(204)
        assertNotNull(created)

        dao.deleteTeamMember(created!!)

        val deleted = dao.getTeamMemberById(204)
        assertNull("Deleted member should no longer exist in database", deleted)
    }

    @Test
    fun `test isInternMember distinguishes regular staff from interns`() {
        val units = listOf(
            CxUnit(id = 1, name = "Service Quality", code = "SQ", description = "", unitHeadName = "", headEmail = "", colorHex = "#008269"),
            CxUnit(id = 5, name = "CX Interns & Trainees", code = "INT", description = "", unitHeadName = "", headEmail = "", colorHex = "#7C3AED")
        )

        val regularStaff = TeamMember(
            id = 1,
            unitId = 1,
            fullName = "Ajmal Hussain",
            employeeId = "SQ-101",
            email = "ajmal@example.com",
            role = "Team Member",
            designation = "Senior Service Quality Lead"
        )
        assertFalse(isInternMember(regularStaff, units))

        val internByRole = TeamMember(
            id = 2,
            unitId = 1,
            fullName = "Farhan Tariq",
            employeeId = "SQ-109",
            email = "farhan@example.com",
            role = "Intern",
            designation = "SQ Intern"
        )
        assertTrue(isInternMember(internByRole, units))

        val internByUnit = TeamMember(
            id = 3,
            unitId = 5,
            fullName = "Fatima Noor",
            employeeId = "INT-402",
            email = "fatima@example.com",
            role = "CX Intern",
            designation = "Customer Experience Trainee"
        )
        assertTrue(isInternMember(internByUnit, units))
    }
}
