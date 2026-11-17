package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.AiNutritionService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Zevra", appName)
  }

  @Test
  fun `local image analysis does not invent a meal for an unrecognized image`() {
    val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

    val result = AiNutritionService().analyzeBitmapVisually(bitmap)

    assertTrue(result.items.isEmpty())
    assertEquals(0, result.totalCalories)
    assertEquals("Ingen mat identifierades", result.mealName)
  }
}
