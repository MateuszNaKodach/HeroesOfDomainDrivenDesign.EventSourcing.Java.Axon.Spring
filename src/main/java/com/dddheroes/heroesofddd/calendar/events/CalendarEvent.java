package com.dddheroes.heroesofddd.calendar.events;

public sealed interface CalendarEvent permits DayStarted, DayFinished {

    String calendarId();
}
