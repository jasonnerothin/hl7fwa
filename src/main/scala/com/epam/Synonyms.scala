package com.epam

/**
  * Synonyms for drug names
  */
trait Synonyms {

  private val inderal: Set[String] = Set("Inderal", "PROPRANOLOL", "Avlocardyl", "Betadren", "Bedranol")
  private val syns: Set[(String, Set[String])] = inderal map { name => name -> inderal }

  /**
    * @param name a drug name
    * @return a set of all synonyms (or just the drug name, if none are known)
    */
  def of(name: String): Set[String] = {
    syns.find {
      _._1 == name
    } match {
      case Some(x) => x._2
      case None => Set(name)
    }
  }

}

object Synonyms extends Synonyms

