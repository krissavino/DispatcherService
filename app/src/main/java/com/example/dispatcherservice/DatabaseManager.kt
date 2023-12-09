package com.example.dispatcherservice
import android.os.StrictMode
import android.util.Log
import android.widget.TableRow
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.io.*
import java.sql.CallableStatement
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Types
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class DatabaseManager {

    companion object {
        /*val url = "jdbc:mysql://192.168.1.112:3306/savinov_db?verifyServerCertificate=false&amp;allowMultiQueries=true";
        val username = "root"
        val password = "1234"*/
        //val url = "jdbc:mysql://10.50.0.5:3306/savinov_db?verifyServerCertificate=false&amp;allowMultiQueries=true&amp;connectTimeout=3000&socketTimeout=30000&amp;characterEncoding=UTF-8";
        val url = "jdbc:mysql://10.50.0.5:3306/savinov_db?characterEncoding=utf8"
        //val url = "jdbc:mariadb://localhost:3306/savinov_db?characterEncoding=utf8"
        val username = "root"
        val password = "TM4vyrAP\$m^c"
        val driver = "com.mysql.jdbc.Driver"
        //val driver = "org.mariadb.jdbc.Driver"
        private fun getConnection(): Connection {
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            Class.forName(driver);
            return DriverManager.getConnection(url, username, password);
        }

        fun getUser(phone: String = ""): UserInfo? {
            try {
                val connection = getConnection()
                val statement = connection.createStatement()
                var resultQuery = statement.executeQuery("call get_user($phone)")
                if (!resultQuery.next()) {
                    connection.close()
                    return null
                }
                Log.i("sql_info", resultQuery.getString("phone"))
                val tmpRes0 = resultQuery.getString("userType")
                val tmpRes8 = resultQuery.getInt("id")
                val tmpRes1 = resultQuery.getString("phone")
                val tmpRes2 = resultQuery.getString("passwd")
                val tmpRes3 = resultQuery.getString("firstName")
                val tmpRes4 = resultQuery.getString("secondName")
                val tmpRes5 = resultQuery.getString("middleName")
                val tmpRes6 =
                    if (tmpRes0 == "Client") resultQuery.getString("address") else "Без адреса"
                connection.close()
                return UserInfo(
                    tmpRes8,
                    tmpRes1,
                    tmpRes2,
                    tmpRes3,
                    tmpRes4,
                    tmpRes5,
                    tmpRes6,
                    tmpRes0
                )
            } catch (e: Exception) {
                Log.e("sql_error1", e.localizedMessage)
            }
            return UserInfo()
        }

        fun registerUser(userInfo: UserInfo): Boolean {
            var con: Connection? = null
            var registerState = false
            try {
                con = getConnection()
                val cs: CallableStatement = con.prepareCall("{?=call register_user(?,?,?,?,?,?)}")
                cs.registerOutParameter(1, Types.BOOLEAN)
                cs.setString(2, userInfo.phone)
                cs.setString(3, userInfo.password)
                cs.setString(4, userInfo.firstName)
                cs.setString(5, userInfo.secondName)
                cs.setString(6, userInfo.middleName)
                cs.setString(7, userInfo.address)
                cs.execute()
                registerState = cs.getBoolean(1)
                Log.i("sql_info", "sql_result: $registerState")
                con?.close()
            } catch (e: java.lang.Exception) {
                Log.e("sql_error", e.localizedMessage)
            }
            return registerState
        }

        fun loginUser(userInfo: UserInfo): String {
            var con: Connection? = null
            var loginState: String = "None"
            try {
                con = getConnection()
                val cs: CallableStatement = con.prepareCall("{?=call login_user(?,?)}")
                cs.registerOutParameter(1, Types.BOOLEAN)
                cs.setString(2, userInfo.phone)
                cs.setString(3, userInfo.password)
                cs.execute()
                loginState = cs.getString(1)
                Log.i("sql_info", "sql_result: $loginState")
                con.close()
            } catch (e: java.lang.Exception) {
                Log.e("sql_error", e.localizedMessage)
            }
            return loginState
        }

        fun updateUser(userInfo: UserInfo): Boolean {
            val connection = getConnection()
            val statement = connection.createStatement()
            try {
                val resultSet = statement.executeQuery("call update_user(${userInfo.id},\'${userInfo.phone}\',\'${userInfo.password}\',\'${userInfo.address}\'," +
                        "\'${userInfo.firstName}\'," +
                        "\'${userInfo.secondName}\',\'${userInfo.middleName}\');")
            } catch (e: java.lang.Exception) {
                Log.e("mysql_error_upduser", e.localizedMessage)
                connection.close()
                return false
            }
            connection.close()
            return true
        }

        fun getDispatcherInfo(): Pair<ArrayList<UserRequest>, MutableMap<Int, UserInfo>> {
            try {
                val connection = getConnection()
                val statement = connection.createStatement()
                val resultSet = statement.executeQuery("call get_dispatcher_requests();")
                var tmpUserRequests: ArrayList<UserRequest> = arrayListOf()
                var tmpUserInfos: MutableMap<Int, UserInfo> = mutableMapOf()
                while (resultSet.next()) {
                    var tmpUserRequest = UserRequest(
                        state = RequestState.valueOf(resultSet.getString("state") ?: "New"),
                        accidentType = resultSet.getString("accidentType") ?: "Без типа",
                        date = SimpleDateFormat("yyyy-MM-dd").parse(resultSet.getString("accidentDate") ?: "2023-01-01"),
                        description = resultSet.getString("description") ?: "Без описания",
                        id = resultSet.getInt("idRequest") ?: 1
                    )
                    var tmpUserInfo = UserInfo(
                        address = resultSet.getString("address"),
                        phone = resultSet.getString("phone"),
                        firstName = resultSet.getString("firstName"),
                        secondName = resultSet.getString("secondName"),
                        middleName = resultSet.getString("middleName")
                    )
                    tmpUserRequests.add(tmpUserRequest)
                    tmpUserInfos[resultSet.getInt("idRequest")] = tmpUserInfo
                }
                connection.close()
                return Pair(tmpUserRequests, tmpUserInfos)
            } catch (e: Exception) {
                Log.e("sql_error_getdispinfo", e.localizedMessage)
            }
            return Pair(arrayListOf(), mutableMapOf())
        }

        fun getUserRequests(userInfo: UserInfo): ArrayList<UserRequest> {
            try {
                val connection = getConnection()
                val statement = connection.createStatement()
                val resultSet = statement.executeQuery("call get_user_requests(${userInfo.phone});")
                var tmpUserRequests: ArrayList<UserRequest> = arrayListOf()
                while (resultSet.next()) {
                    var tmpUserRequest = UserRequest()
                    tmpUserRequest.state = RequestState.valueOf(resultSet.getString("state"))
                    tmpUserRequest.date =
                        (SimpleDateFormat("yyyy-MM-dd").parse(resultSet.getString("accidentDate")))
                    tmpUserRequest.description = resultSet.getString("description")
                    tmpUserRequests.add(tmpUserRequest)
                }
                connection.close()
                return tmpUserRequests
            } catch (e: java.lang.Exception) {
                Log.e("sql_error_getusreq", e.localizedMessage)
            }
            return arrayListOf()
        }

        fun sendRequest(userInfo : UserInfo, userRequest: UserRequest) : Boolean {
            val connection = getConnection()
            val statement = connection.createStatement()
            try {
                statement.executeUpdate("call send_user_request(" +
                        "${userInfo.id}, \'${userRequest.accidentType}\', \'${userRequest.state}\', " +
                        "\"${SimpleDateFormat("yyyy-MM-dd").format(userRequest.date)}\", \'${userRequest.description}\'" +
                        ");")
            } catch (e : java.lang.Exception) {
                Log.e("mysql_send_error", e.localizedMessage)
                connection.close()
                return false
            }
            connection.close()
            return true
        }

        fun updateRequest(userInfo : UserInfo, userRequest: UserRequest) : Boolean {
            val connection = getConnection()
            val statement = connection.createStatement()
            try {
                statement.executeUpdate("call update_user_request(" +
                        "${userRequest.id}, ${userInfo.id}, \'${userRequest.accidentType}\', \'${userRequest.state}\', \"${SimpleDateFormat("yyyy-MM-dd").format(userRequest.date)}\", \'${userRequest.description}\'" +
                        ");")
            } catch (e : java.lang.Exception) {
                Log.e("mysql_updreq_error", e.localizedMessage)
                connection.close()
                return false
            }
            connection.close()
            return true
        }

        fun deleteRequest(userRequest: UserRequest) {
            val connection = getConnection()
            val statement = connection.createStatement()
            try {
                statement.executeUpdate("call delete_user_request(${userRequest.id});")
            } catch (e : java.lang.Exception) {
                Log.e("mysql_delete_error", e.localizedMessage)
                connection.close()
            }
            connection.close()
        }

        fun sqlRequest(request : String) : SnapshotStateMap<String, SnapshotStateList<String>> {
            var result = mutableStateMapOf<String, SnapshotStateList<String>>()
            try {
                val connection = getConnection()
                val statement = connection.createStatement()
                try {
                    if(request.contains("update") || request.contains("insert") || request.contains("delete")) {
                        val resultSet = statement.executeUpdate(request)
                    } else {
                        val resultSet = statement.executeQuery(request)
                        val resultMeta = resultSet.metaData
                        for (i in 1..resultMeta.columnCount) {
                            result[resultMeta.getColumnName(i)] = mutableStateListOf()
                        }
                        while (resultSet.next()) {
                            for (i in 1..resultMeta.columnCount) {
                                result[resultMeta.getColumnName(i)]?.add(resultSet.getString(i))
                            }
                        }
                    }
                } catch(e : Exception) {
                    Log.e("mysql_sqlsend_error", e.localizedMessage)
                    result = mutableStateMapOf(Pair(e.localizedMessage, mutableStateListOf()))
                } finally {
                    connection.close()
                }
            } catch (e : java.lang.Exception) {
                Log.e("mysql_oldconnect_error", e.localizedMessage)
            }
            return result
        }
    }
}