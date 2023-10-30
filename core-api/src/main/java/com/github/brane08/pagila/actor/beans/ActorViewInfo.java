package com.github.brane08.pagila.actor.beans;


import java.io.Serial;
import java.io.Serializable;

public class ActorViewInfo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    long actorId;
    String firstName;
    String lastName;
    String filmInfo;


    public long getActorId() {
        return actorId;
    }

    public void setActorId(long actorId) {
        this.actorId = actorId;
    }


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }


    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }


    public String getFilmInfo() {
        return filmInfo;
    }

    public void setFilmInfo(String filmInfo) {
        this.filmInfo = filmInfo;
    }

}
