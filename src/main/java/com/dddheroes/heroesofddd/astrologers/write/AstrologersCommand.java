package com.dddheroes.heroesofddd.astrologers.write;

import com.dddheroes.heroesofddd.shared.slices.write.Command;

public interface AstrologersCommand extends Command {

    AstrologersId astrologersId();
}
