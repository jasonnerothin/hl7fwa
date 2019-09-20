package com.epam.helper
import com.typesafe.config.Config

/**
  * <WORK> (c) by <AUTHOR(S)>
  *
  * <WORK> is licensed under a
  * Creative Commons Attribution-ShareAlike 4.0 International License.
  *
  * You should have received a copy of the license along with this
  *   work.  If not, see <http://creativecommons.org/licenses/by-sa/4.0/>.
  */
trait PgUrl {

  def pgConnectionString()(implicit conf: Config): String = {
    val host = conf.getString("postgres.host")
    val port = conf.getString("postgres.port")
    val dbName = conf.getString("postgres.dbname")
    s"jdbc:postgresql://$host:$port/$dbName"
  }
}
