package br.edu.ifpe.todo.util

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.currentLocale

actual fun formatDateTime(millis: Long): String {
    val formatter = NSDateFormatter().apply {
        dateStyle = NSDateFormatterMediumStyle
        timeStyle = NSDateFormatterShortStyle
        locale = currentLocale()
    }
    val date = NSDate.dateWithTimeIntervalSince1970(millis / 1000.0)
    return formatter.stringFromDate(date)
}