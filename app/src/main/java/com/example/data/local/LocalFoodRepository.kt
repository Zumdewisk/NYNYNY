package com.example.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Repository abstracting local database access for [FoodItem] entities.
 */
class LocalFoodRepository(private val foodItemDao: FoodItemDao) {

    val allFoodItems: Flow<List<FoodItem>> = foodItemDao.getAllFoodItems()

    fun getFoodItemsByDate(date: String): Flow<List<FoodItem>> =
        foodItemDao.getFoodItemsByDate(date)

    fun getFoodItemsByDateAndMealType(date: String, mealType: String): Flow<List<FoodItem>> =
        foodItemDao.getFoodItemsByDateAndMealType(date, mealType)

    fun getFoodItemsByMealId(mealId: String): Flow<List<FoodItem>> =
        foodItemDao.getFoodItemsByMealId(mealId)

    fun getFoodItemById(id: Long): Flow<FoodItem?> =
        foodItemDao.getFoodItemById(id)

    fun getTotalCaloriesForDate(date: String): Flow<Int> =
        foodItemDao.getTotalCaloriesForDate(date)

    fun getTotalProteinForDate(date: String): Flow<Double> =
        foodItemDao.getTotalProteinForDate(date)

    fun getTotalCarbsForDate(date: String): Flow<Double> =
        foodItemDao.getTotalCarbsForDate(date)

    fun getTotalFatForDate(date: String): Flow<Double> =
        foodItemDao.getTotalFatForDate(date)

    suspend fun insert(foodItem: FoodItem): Long =
        foodItemDao.insert(foodItem)

    suspend fun insertAll(foodItems: List<FoodItem>): List<Long> =
        foodItemDao.insertAll(foodItems)

    suspend fun update(foodItem: FoodItem) =
        foodItemDao.update(foodItem)

    suspend fun delete(foodItem: FoodItem) =
        foodItemDao.delete(foodItem)

    suspend fun deleteById(id: Long) =
        foodItemDao.deleteById(id)

    suspend fun deleteByMealId(mealId: String) =
        foodItemDao.deleteByMealId(mealId)

    suspend fun deleteAllForDate(date: String) =
        foodItemDao.deleteAllForDate(date)

    suspend fun clearAll() =
        foodItemDao.clearAll()
}
