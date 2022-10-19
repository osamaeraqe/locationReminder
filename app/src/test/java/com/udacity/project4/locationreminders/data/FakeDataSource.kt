package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(): ReminderDataSource {

    private var shouldReturnError = false
    var remindersList: MutableList<ReminderDTO> = mutableListOf()

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        try {
            if (shouldReturnError) {
                return Result.Error(
                    "Error : DataBase"
                )
            }
           return Result.Success(remindersList.toList())
        }
        catch (ex : Exception){
          return  Result.Error(ex.localizedMessage)
        }

    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        try {
            val reminder = remindersList.find { reminderDTO ->
                reminderDTO.id == id
            }

            return when {
                shouldReturnError -> {
                    Result.Error("Error : Database ")
                }

                reminder != null -> {
                    Result.Success(reminder)
                }
                else -> {
                    Result.Error("Reminder not found!")
                }
            }
        }
        catch (ex:Exception){return  Result.Error(ex.localizedMessage)}
    }

    override suspend fun deleteAllReminders() {
        remindersList.clear()
    }
    fun clearalldata(){
        remindersList.clear()
    }

}