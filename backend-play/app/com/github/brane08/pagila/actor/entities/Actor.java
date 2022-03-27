package com.github.brane08.pagila.actor.entities;

import com.github.brane08.pagila.seedworks.entities.BaseModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "actor")
public class Actor extends BaseModel {
	@Id
	@Column(name = "actor_id")
	public Integer actorId;

	@Column(name = "first_name")
	public String firstName;

	@Column(name = "last_name")
	public String lastName;

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Actor that = (Actor) o;
		return actorId.equals(that.actorId) &&
				Objects.equals(firstName, that.firstName) &&
				Objects.equals(lastName, that.lastName) &&
				Objects.equals(lastUpdate, that.lastUpdate);
	}

	@Override
	public int hashCode() {
		return Objects.hash(actorId, firstName, lastName, lastUpdate);
	}
}
