package com.dddheroes.heroesofddd.astrologers.events;

public sealed interface AstrologersEvent permits WeekSymbolProclaimed {

    String astrologersId();
} 