package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for [FoodItem] entity.
 * Provides reactive Flow queries and coroutine-based write operations for local nutrition storage.
 */
@Dao
interface FoodItemDao {

    @Query("SELECT * FROM food_items ORDER BY logged_at DESC")
    fun getAllFoodItems(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE date = :date ORDER BY logged_at ASC")
    fun getFoodItemsByDate(date: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE date = :date AND meal_type = :mealType ORDER BY logged_at ASC")
    fun getFoodItemsByDateAndMealType(date: String, mealType: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE meal_id = :mealId ORDER BY logged_at ASC")
    fun getFoodItemsByMealId(mealId: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    fun getFoodItemById(id: Long): Flow<FoodItem?>

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun getFoodItemByIdDirect(id: Long): FoodItem?

    @Query("SELECT COALESCE(SUM(calories), 0) FROM food_items WHERE date = :date")
    fun getTotalCaloriesForDate(date: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(protein), 0.0) FROM food_items WHERE date = :date")
    fun getTotalProteinForDate(date: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(carbs), 0.0) FROM food_items WHERE date = :date")
    fun getTotalCarbsForDate(date: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(fat), 0.0) FROM food_items WHERE date = :date")
    fun getTotalFatForDate(date: String): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(foodItem: FoodItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foodItems: List<FoodItem>): List<Long>

    @Update
    suspend fun update(foodItem: FoodItem)

    @Delete
    suspend fun delete(foodItem: FoodItem)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM food_items WHERE meal_id = :mealId")
    suspend fun deleteByMealId(mealId: String)

    @Query("DELETE FROM food_items WHERE date = :date")
    suspend fun deleteAllForDate(date: String)

    @Query("DELETE FROM food_items")
    suspend fun clearAll()
}
