package com.wenyan.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.WritingMaterialEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WritingMaterialUpsertTest {
    private lateinit var db: WenyanDatabase

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(), WenyanDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After fun tearDown() = db.close()

    @Test fun `updating a material never cascade-deletes its provenance`() = runTest {
        val original = WritingMaterialEntity("wm", "EVIDENCE", null, "old", null, null, 1)
        db.writingMaterialDao().insert(original)
        db.dataSourceDao().insert(
            DataSourceEntity("source", null, null, "", null, "USER_CREATED", createdAt = 1, writingMaterialId = "wm"),
        )

        db.writingMaterialDao().insert(original.copy(content = "new"))

        assertEquals("new", db.writingMaterialDao().getById("wm")?.content)
        assertEquals("wm", db.dataSourceDao().getById("source")?.writingMaterialId)
    }
}
