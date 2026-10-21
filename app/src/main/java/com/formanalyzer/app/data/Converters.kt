package com.formanalyzer.app.data

import androidx.room.TypeConverter
import com.formanalyzer.app.squat.SquatErrorType

class Converters {
    @TypeConverter
    fun errorsToString(errors: Set<SquatErrorType>): String =
        errors.joinToString(",") {it.name }

    @TypeConverter
    fun stringToErrors(value: String): Set<SquatErrorType> =

        if(value.isEmpty()){
            emptySet()
        }else{
            value.split(",").map { SquatErrorType.valueOf(it) }.toSet()
        }



}