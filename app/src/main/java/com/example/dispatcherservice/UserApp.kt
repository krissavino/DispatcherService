package com.example.dispatcherservice

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Paint.Align
import android.os.Bundle
import android.util.Log
import android.widget.DatePicker
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay
import java.util.Date


enum class CurrentScreen {
    Register,
    Login,
    //User
    UserRequests,
    AddingRequest,
    UserProfile,
    DispatcherRequest,
    DispatcherRequests,
    DispatcherSQL
}

enum class RequestState {
    New,
    Process,
    Done
}
class MainActivity : ComponentActivity() {

    init {
        instance = this
    }
    companion object {
        public var userInfo : UserInfo = UserInfo()
        public var userRequests : ArrayList<UserRequest> = arrayListOf(UserRequest())
        public var dispatcherRequest : UserRequest = UserRequest()
        public var dispatcherSQLResult : String = ""
        public var dispatcherUsers : MutableMap<Int, UserInfo> = mutableMapOf()
        public var currentScreen : CurrentScreen = CurrentScreen.Login
        private var instance: MainActivity? = null
        fun applicationContext() : Context {
            return instance!!.applicationContext
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.setProperty("file.encoding", "UTF8")
        val context : Context = MainActivity.applicationContext()
        setContent {
            UserApp()
        }
    }
}

@Composable
private fun UserApp(modifier : Modifier = Modifier) {
    var currentScreen by rememberSaveable { mutableStateOf(MainActivity.currentScreen) }
    Surface(modifier) {
        Column {
            if(currentScreen == CurrentScreen.UserRequests || currentScreen == CurrentScreen.UserProfile
                || currentScreen == CurrentScreen.DispatcherRequests
                || currentScreen == CurrentScreen.DispatcherSQL) {
                MenuButtons(onClick = { screenType ->
                    currentScreen = screenType
                })
            }
            when (currentScreen) {
                CurrentScreen.Register -> {
                    RegisterScreen(
                        onContinueClicked = { usIn ->
                            MainActivity.userInfo = usIn
                            Log.i("sql_info_user", "continue clicked ")
                            currentScreen = CurrentScreen.UserRequests
                        },
                        onBackClicked = { currentScreen = CurrentScreen.Login }
                    )
                }
                CurrentScreen.Login -> {
                    LoginScreen(
                        onLoginUser = { currentScreen = CurrentScreen.UserRequests },
                        onLoginDispatcher = {
                            getDispatcherRequests()
                            currentScreen = CurrentScreen.DispatcherRequests
                        },
                        onRegisterClicked = {
                            currentScreen = CurrentScreen.Register
                        }
                    )
                }
                CurrentScreen.AddingRequest -> {
                    AddingScreen(
                        onAddClicked = { userRequest ->
                            DatabaseManager.sendRequest(MainActivity.userInfo,userRequest)
                            currentScreen = CurrentScreen.UserRequests
                        },
                        onBackClicked = { currentScreen = CurrentScreen.UserRequests }
                    )
                }
                CurrentScreen.UserProfile -> {
                    UserProfileScreen(
                        onEditClick = {
                            Thread {
                                DatabaseManager.updateUser(MainActivity.userInfo)
                            }.start()
                            Toast.makeText(MainActivity.applicationContext(),"Сохранено",Toast.LENGTH_SHORT).show()
                        },
                        onExitClick = {
                            currentScreen = CurrentScreen.Login
                        }
                    )
                }
                CurrentScreen.UserRequests -> {
                    UserRequestsScreen(
                        onAddClick = {  currentScreen = CurrentScreen.AddingRequest  }
                    )
                }
                CurrentScreen.DispatcherRequests -> {
                    DispatcherRequestsScreen(
                        onViewClicked = { request ->
                            MainActivity.dispatcherRequest = request
                            currentScreen = CurrentScreen.DispatcherRequest
                        })
                }
                CurrentScreen.DispatcherRequest -> {
                    DispatcherRequestScreen(
                        onBackClicked = { currentScreen = CurrentScreen.DispatcherRequests },
                        onSaveClicked = {
                            var request = MainActivity.dispatcherRequest
                            var userInfo = MainActivity.dispatcherUsers[request.id] ?: UserInfo()
                            Thread { DatabaseManager.updateRequest(userInfo,request) }.start()
                            currentScreen = CurrentScreen.DispatcherRequests
                        },
                        onDeleteClicked = {
                            var request = MainActivity.dispatcherRequest
                            Thread { DatabaseManager.deleteRequest(request) }.start()
                            currentScreen = CurrentScreen.DispatcherRequests
                        }
                    )
                }
                CurrentScreen.DispatcherSQL -> {
                    DispatcherSQLScreen(
                        onBackClicked = { currentScreen = CurrentScreen.DispatcherRequests }
                    )
                }

                else -> {

                }
            }
        }
    }
}

@Composable
fun MenuButtons(onClick: (screenType : CurrentScreen) -> Unit) {
    var screenType = if(MainActivity.userInfo.profession == "Client")
        listOf(CurrentScreen.UserProfile, CurrentScreen.UserRequests)
    else
        listOf(CurrentScreen.UserProfile, CurrentScreen.DispatcherRequests, CurrentScreen.DispatcherSQL)
    var (selectedType, onSelectType) = rememberSaveable { mutableStateOf(screenType[1]) }
    Row(modifier = Modifier.fillMaxWidth()) {
        screenType.forEach { text ->
            Column( modifier = Modifier.weight(
                if(MainActivity.userInfo.profession == "Client") 0.5f else 0.33f)
            ) {
                OutlinedButton(
                    onClick = {
                        selectedType = text
                        onClick(selectedType)
                        onSelectType(text)
                    },
                    colors = if (selectedType == text) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    },
                    shape = RectangleShape,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when(text) {
                            CurrentScreen.UserProfile -> "Профиль"
                            CurrentScreen.DispatcherSQL -> "SQL"
                            else -> "Запросы"
                        }
                    )
                }
            }
        }
    }
}

open class UserRequest(state : RequestState = RequestState.New,
                       accidentType : String = "Без типа",
                       date : Date = Calendar.getInstance().time,
                       description : String = "Без описания",
                       id : Int = 0
) {
    var state : RequestState = state
    var accidentType : String = accidentType
    var date : Date = date
    var description = description
    var id = id
}

class UserInfo(
    id : Int = 0,
    phone : String = "88005553535",
    password : String = "1234",
    firstName : String = "Иван",
    secondName : String = "Иванов",
    middleName : String = "Иванович",
    address : String = "Садовая 54",
    profession : String = "Client"
) {
    var id = id
    var password : String = password
    var firstName : String = firstName
    var secondName : String = secondName
    var middleName : String = middleName
    var address : String = address
    var phone : String = phone
    var profession : String = profession
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun UserRequestsScreen(
    modifier : Modifier = Modifier,
    onAddClick: () -> Unit,
) {
    Column {
        Box(Modifier.fillMaxSize()) {
            UserRequests()
            Box(
                Modifier.fillMaxSize()
            ) {
                FloatingActionButton(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    content = {
                        Icon(Icons.Filled.Add, contentDescription = "Добавить")
                    },
                    onClick = onAddClick
                )
            }
        }

    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float
) {
    Text(
        text = text,
        modifier = Modifier
            .border(1.dp, Color.Black)
            .weight(weight)
            .padding(8.dp)
            .height(40.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DispatcherSQLScreen(
    onBackClicked : () -> Unit
) {
    BackHandler() {
        onBackClicked()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SQL запрос в БД",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth()
            )
            Spacer(modifier = Modifier.padding(all=5.dp))
            var sqlQuery : String by rememberSaveable{ mutableStateOf("select * from client;") }
            TextField(
                label = { Text("SQL запрос") },
                value = sqlQuery,
                onValueChange = { sqlQuery = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            var sqlResult = remember { mutableStateMapOf<String, MutableList<String>>() }
            Button(onClick = {
                val tmpResult = DatabaseManager.sqlRequest(sqlQuery)
                sqlResult.clear()
                tmpResult.keys.forEach { key ->
                    sqlResult[key] = mutableStateListOf()
                }
                tmpResult.keys.forEach {it1 ->
                    tmpResult[it1]?.forEach { it2 ->
                        sqlResult[it1]?.add(it2)
                    }
                }
            }) {
                Text(
                    text = "Отправить"
                )
            }
            Spacer(modifier = Modifier.padding(all=5.dp))
            // The LazyColumn will be our table. Notice the use of the weights below
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)) {
                // Here is the header
                item {
                    Row() {
                        sqlResult.keys.forEach {
                            TableCell(text=it, 0.5f)
                        }
                    }
                }

                items(1) { item ->
                    var i = 0
                    sqlResult.values.forEach { it1 ->
                        Row() {
                            sqlResult.keys.forEach { it2 ->
                                TableCell(text = sqlResult[it2]?.get(i) ?: "", weight = 0.5f)
                            }
                        }
                        i += 1
                    }
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun DispatcherRequestsScreen(onViewClicked: (request : UserRequest) -> Unit) {
    Column {
        Box(Modifier.fillMaxSize()) {
            DispatcherRequests(onViewClicked =onViewClicked)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DispatcherRequestScreen(
    onBackClicked : () -> Unit,
    onSaveClicked : () -> Unit,
    onDeleteClicked : () -> Unit
) {
    var request = MainActivity.dispatcherRequest
    var userInfo = MainActivity.dispatcherUsers[request.id] ?: UserInfo()
    Log.i("wtf!", "wtf_screen")
    var description by rememberSaveable { mutableStateOf(request.description) }
    BackHandler() {
        onBackClicked()
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Авария №${request.id}",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(all = 4.dp)
                    .fillMaxWidth()
            )
            Text(
                text = "Статус",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .fillMaxWidth()
            )
            Row() {
                val requestStates = RequestState.values()
                val (selectedType, onTypeSelected) = rememberSaveable { mutableStateOf(MainActivity.dispatcherRequest.state) }
                requestStates.forEach { text ->
                    OutlinedButton(
                        onClick = {
                            onTypeSelected(text)
                            MainActivity.dispatcherRequest.state = text
                        },
                        colors = if (selectedType == text) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        },
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            fontSize=11.sp,
                            text = when(text) {
                                RequestState.Done -> "Выполнен"
                                RequestState.Process -> "В процессе"
                                else -> "Новый"
                            }
                        )
                    }
                }
            }
            var menuBoxExpanded by remember {mutableStateOf(false)}
            val accidentTypes = listOf("Холодное водоснабжение", "Горячее водоснабжение", "Газ", "Электричество", "Другое")
            var selectedType = request.accidentType
            Box(modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = menuBoxExpanded,
                    onExpandedChange = { menuBoxExpanded = !menuBoxExpanded }) {
                    TextField(value = selectedType, textStyle = TextStyle.Default.copy(fontSize=13.sp),
                        onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuBoxExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .align(Alignment.Center)
                    )
                    ExposedDropdownMenu(
                        expanded = menuBoxExpanded,
                        onDismissRequest = { menuBoxExpanded = false }
                    ) {
                        accidentTypes.forEach { item ->
                            DropdownMenuItem(text = { Text(text = item) }, onClick = {
                                selectedType = item
                                MainActivity.dispatcherRequest.accidentType = item
                                menuBoxExpanded = false
                            })
                        }
                    }
                }
            }
            Text(
                text = "Тип: ${request.accidentType}\nАдрес: ${userInfo.address}\n" +
                        "Время: ${SimpleDateFormat("yyyy-MM-dd").format(request.date.time)}",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Left,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = {
                    if (it.length < 500) description = it
                    MainActivity.dispatcherRequest.description = it
                },
                label = { Text("Описание") },
                singleLine = false,
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(start = 15.dp, top = 5.dp, end = 15.dp)
            )
            Button(onClick = onSaveClicked) {
                Text(
                    text = "Сохранить"
                )
            }
            Button(onClick = onDeleteClicked) {
                Text(
                    text = "Удалить запрос"
                )
            }
            Text(
                text = "Информация о пользователе",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Left,
                modifier = Modifier
                    .fillMaxWidth()
            )
            Text(
                text = "${userInfo.secondName + " " + userInfo.firstName + " " + userInfo.middleName}\n" +
                        "Номер телефона: ${userInfo.phone}",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Left,
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 4.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DispatcherRequestsFilter(filterClicked : (selectedDate : String, selectedAccident : String, selectedState : String, searchWords : List<String>) -> Unit) {
    var searchWords by rememberSaveable { mutableStateOf("") }

    var menuBox1Expanded by remember {mutableStateOf(false)}
    val accidentTypes = listOf("Не выбрано","Холодное водоснабжение", "Горячее водоснабжение", "Газ", "Электричество", "Другое")
    var accidentType by remember {mutableStateOf(accidentTypes[0])}
    var menuBox2Expanded by remember {mutableStateOf(false)}
    val stateTypes = listOf("Не выбрано","Новый", "В процессе", "Выполнен")
    var stateType by remember { mutableStateOf(stateTypes[0]) }

    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    calendar.time = Date()
    val date = remember { mutableStateOf("Не выбрано") }
    val datePickerDialog = DatePickerDialog(
        LocalContext.current,
        { _: DatePicker, year:Int, month :Int, dayOfMonth: Int ->
            var varmonth = month + 1
            var tmp = varmonth.toString()
            var tmp2 = dayOfMonth.toString()
            if(varmonth < 10)
                tmp = "0$tmp"
            if(dayOfMonth < 10)
                tmp2 = "0$tmp2"
            date.value = "$year-$tmp-$tmp2"
            var tmpSW = searchWords.split(" ").toMutableList()
            tmpSW.removeIf { sit -> sit == "" }
            if(tmpSW.isEmpty()) tmpSW = mutableListOf(searchWords)
            filterClicked(date.value,accidentType,stateType,tmpSW)
        }, year, month, day
    )

    Column() {
        Column(horizontalAlignment = Alignment.Start) {
            var showFilter by remember { mutableStateOf(false) }
            Button(
                onClick = {
                    showFilter = !showFilter
                    if(!showFilter) {
                        searchWords = ""
                        date.value = "Не выбрано"
                        accidentType = "Не выбрано"
                        stateType = "Не выбрано"
                        filterClicked(date.value,accidentType,stateType,mutableListOf(""))
                    }
                },
                modifier=Modifier.padding(5.dp)
            ) {
                Text(text = "Фильтр")
            }
            if(showFilter) {
                Button(onClick = {
                    datePickerDialog.show()
                },
                    modifier = Modifier.padding(5.dp)) {
                    Text("Дата: ${date.value}")
                }
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = menuBox1Expanded,
                        onExpandedChange = { menuBox1Expanded = !menuBox1Expanded }
                    ) {
                        TextField(
                            value = accidentType,
                            textStyle = TextStyle.Default.copy(fontSize = 13.sp),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuBox1Expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .align(Alignment.Center)
                        )
                        ExposedDropdownMenu(
                            expanded = menuBox1Expanded,
                            onDismissRequest = { menuBox1Expanded = false }
                        ) {
                            accidentTypes.forEach { item ->
                                DropdownMenuItem(text = { Text(text = item) }, onClick = {
                                    accidentType = item
                                    var tmpSW = searchWords.split(" ").toMutableList()
                                    tmpSW.removeIf { sit -> sit == "" }
                                    if (tmpSW.isEmpty()) tmpSW = mutableListOf(searchWords)
                                    filterClicked(date.value, accidentType, stateType, tmpSW)
                                    menuBox1Expanded = false
                                }, modifier = Modifier.align(Alignment.CenterHorizontally))
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(5.dp)
                        .fillMaxWidth()
                ) {
                    ExposedDropdownMenuBox(
                        expanded = menuBox2Expanded,
                        onExpandedChange = { menuBox2Expanded = !menuBox2Expanded }
                    ) {
                        TextField(
                            value = stateType, textStyle = TextStyle.Default.copy(fontSize = 13.sp),
                            onValueChange = {}, readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuBox2Expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .align(Alignment.Center)
                        )
                        ExposedDropdownMenu(
                            expanded = menuBox2Expanded,
                            onDismissRequest = { menuBox2Expanded = false }
                        ) {
                            stateTypes.forEach { item ->
                                DropdownMenuItem(text = { Text(text = item) }, onClick = {
                                    stateType = item
                                    var tmpSW = searchWords.split(" ").toMutableList()
                                    tmpSW.removeIf { sit -> sit == "" }
                                    if (tmpSW.isEmpty()) tmpSW = mutableListOf(searchWords)
                                    filterClicked(date.value, accidentType, stateType, tmpSW)
                                    menuBox2Expanded = false
                                })
                            }
                        }
                    }
                }
            }
            TextField(
                label = { Text("Поиск") },
                value = searchWords,
                onValueChange = {
                    searchWords = it
                    var tmpSW = searchWords.split(" ").toMutableList()
                    tmpSW.removeIf { sit -> sit == "" }
                    if(tmpSW.isEmpty()) tmpSW = mutableListOf(searchWords)
                    filterClicked(date.value,accidentType,stateType,tmpSW)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun getDispatcherRequests() : ArrayList<UserRequest> {
    val tmpPair = DatabaseManager.getDispatcherInfo()
    MainActivity.userRequests = tmpPair.first
    MainActivity.dispatcherUsers = tmpPair.second
    return tmpPair.first
}

private fun getUserRequests() : ArrayList<UserRequest> {
    MainActivity.userRequests = DatabaseManager.getUserRequests(MainActivity.userInfo)
    return MainActivity.userRequests
}

fun filterBySearchWords(requests : List<UserRequest>, searchWords : MutableList<String>) : List<UserRequest> {
    var resultRequests = mutableListOf<UserRequest>()
    requests.forEach { request ->
        searchWords.forEach { word ->
            val tmpState = when(request.state) {
                RequestState.Done -> "Выполнен"
                RequestState.Process -> "В процессе"
                else -> "Новый"
            }
            if(request.description.contains(word,true) || tmpState.contains(word,true) || request.accidentType.contains(word,true)
                || SimpleDateFormat("yyyy-MM-dd").format(request.date.time).contains(word,true)) {
                resultRequests.add(request)
            } else {
                var user = MainActivity.dispatcherUsers[request.id] ?: null
                if(user != null) {
                    if(user.address.contains(word,true) || user.firstName.contains(word,true) || user.secondName.contains(word,true)
                        || user.middleName.contains(word,true) || user.phone.contains(word,true))
                        resultRequests.add(request)
                }
            }
        }
    }
    return resultRequests
}

@Composable
private fun DispatcherRequestsList(
    selectedDate: String, selectedAccident: String, selectedState: String,
    searchWords : MutableList<String>, onViewClicked : (request : UserRequest) -> Unit)
{
    var sortedRequests = remember { MainActivity.userRequests }
    var tmpRequests = sortedRequests.toMutableSet()
    val tmp2Requests = tmpRequests.toMutableSet()
    tmpRequests.clear()
    tmp2Requests.forEach {
        var good1 = true
        var good2 = true
        var good3 = true
        if (selectedDate != "Не выбрано") {
            if (SimpleDateFormat("yyyy-MM-dd").format(it.date) == selectedDate)
            else
                good1 = false
        }
        if (selectedAccident != "Не выбрано") {
            if (it.accidentType == selectedAccident)
            else
                good2 = false
        }
        if (selectedState != "Не выбрано") {
            val tmp = when (it.state) {
                RequestState.Process -> "В процессе"
                RequestState.Done -> "Выполнен"
                else -> "Новый"
            }
            if (tmp == selectedState)
            else
                good3 = false
        }
        if(good1 && good2 && good3)
            tmpRequests.add(it)
    }
    if(searchWords.isNotEmpty()) tmpRequests = filterBySearchWords(tmpRequests.toMutableList(), searchWords).toMutableSet()
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if(isRefreshing) {
            delay(1000)
            isRefreshing = false
        }
    }
    if(tmpRequests.isEmpty()) {
        Text(
            text = "Не найдено запросов удовлетворяющих условиям поиска",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxWidth()
        )
    } else {
        SwipeRefresh(
            state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
            onRefresh = {
                val tmp = getDispatcherRequests()
                sortedRequests.clear()
                tmp.forEach {
                    sortedRequests.add(it)
                }
                isRefreshing = true
            }
        ) {
            LazyColumn() {
                items(tmpRequests.toMutableList()) { item ->
                    DispatcherRequestCard(item, onViewClicked)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DispatcherRequests(onViewClicked: (request: UserRequest) -> Unit) {
    Column {
        val userRequests = listOf(UserRequest())
        if(userRequests.isEmpty()) {
            Text(
                text = "Нет запросов.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(all = 12.dp)
                    .fillMaxWidth()
            )
        } else {
            var selectedDate by remember { mutableStateOf("Не выбрано") }
            var selectedAccident by remember { mutableStateOf("Не выбрано") }
            var selectedState by remember { mutableStateOf("Не выбрано") }
            var searchWords by remember { mutableStateOf(mutableListOf<String>()) }
            DispatcherRequestsFilter(filterClicked = { sd, sa, ss, sw ->
                searchWords = sw.toMutableList()
                selectedDate = sd.toString()
                selectedAccident = sa.toString()
                selectedState = ss
                Log.i("app_info", sw.count().toString())
            })
            DispatcherRequestsList(selectedDate=selectedDate, selectedAccident=selectedAccident, selectedState=selectedState,
                searchWords=searchWords, onViewClicked=onViewClicked)
        }
    }
}

@Composable
private fun UserRequests() {
    Column( ) {
        var userRequests = remember { getUserRequests() }
        if(userRequests.isEmpty()) {
            Text(
                text = "Нет запросов.",
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(all = 12.dp)
                    .fillMaxWidth()
            )
            Button(
                onClick = {

                    userRequests = MainActivity.userRequests
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "Обновить",
                    modifier = Modifier.padding(all=5.dp)
                )
            }
        } else {
            var isRefreshing by remember { mutableStateOf(false) }
            LaunchedEffect(isRefreshing) {
                if(isRefreshing) {
                    delay(1000)
                    isRefreshing = false
                }
            }
            SwipeRefresh(
                state =  rememberSwipeRefreshState(isRefreshing = isRefreshing),
                onRefresh = {
                    val tmp = getUserRequests()
                    userRequests.clear()
                    tmp.forEach {
                        userRequests.add(it)
                    }
                    isRefreshing = true
                }
            ) {
                LazyColumn() {
                    items(userRequests) { item ->
                        UserRequestCard(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatcherRequestCard(request: UserRequest, onViewClicked: (request : UserRequest) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column() {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)) {
                Column() {
                    Text(text = "Статус: ${
                        when(request.state) {
                            RequestState.Done -> "Выполнен"
                            RequestState.Process -> "В процессе"
                            else -> "Новый"
                        }
                    }")
                    Text(text = "Время: ${SimpleDateFormat("yyyy-MM-dd").format(request.date.time)}" )
                    Text(text = "Тип аварии: ${request.accidentType}" )
                }
                Column(modifier = Modifier
                    .fillMaxWidth(),
                    horizontalAlignment = Alignment.End) {
                    ElevatedButton(
                        onClick = { onViewClicked(request) }
                    ) {
                        Text("+")
                    }
                }

            }
        }

    }
}

@Composable
private fun UserRequestCard(request : UserRequest) {
    val expanded = remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column() {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)) {
                Column() {
                    Text(text = "Статус: ${
                        when(request.state) {
                            RequestState.Done -> "Выполнен"
                            RequestState.Process -> "В процессе"
                            else -> "Новый"
                        }
                    }")
                    Text(text = "Время: ${SimpleDateFormat("yyyy-MM-dd").format(request.date.time)}" )
                }
                Column(modifier = Modifier
                    .fillMaxWidth(),
                    horizontalAlignment = Alignment.End) {
                    ElevatedButton(
                        onClick = { expanded.value = !expanded.value },
                    ) {
                        Text("Подробнее")
                    }
                }

            }
            if(expanded.value) {
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, start = 24.dp)) {
                    Text(text="Описание: ${
                        if(request.description == "") "Без описания" else request.description
                    }")
                }
            }
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onContinueClicked: (usIn : UserInfo) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    var password : String by rememberSaveable{ mutableStateOf("") }
    var phone : String by rememberSaveable{ mutableStateOf("") }
    var address : String by rememberSaveable{ mutableStateOf("") }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Диспетчерская служба",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxWidth()
        )
        var fullName : String by rememberSaveable{ mutableStateOf("") }
        TextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Полное имя (необязательно)") }
        )
        TextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Адрес") }
        )
        TextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Номер телефона") }
        )
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.padding(all=4.dp))
        Button(
            onClick = {
                var names = fullName.split(' ')
                val secondName = if (names.isNotEmpty()) names[0] else "Не указано"
                val firstName = if (names.size > 1) names[1] else "Не указано"
                val middleName = if (names.size > 2) names[2] else "Не указано"
                if(address.isEmpty()) {
                    Toast.makeText(MainActivity.applicationContext(),"Укажите адрес",Toast.LENGTH_SHORT).show()
                } else if(phone.isEmpty()) {
                    Toast.makeText(MainActivity.applicationContext(),"Укажите номер телефона",Toast.LENGTH_SHORT).show()
                } else if(password.isEmpty()) {
                    Toast.makeText(MainActivity.applicationContext(), "Введите пароль", Toast.LENGTH_SHORT).show()
                } else {
                    val userInfo = UserInfo(0, phone, password, firstName, secondName, middleName, address)
                    if(DatabaseManager.registerUser(userInfo))
                        onContinueClicked(userInfo)
                    else
                        Log.i("sql_info_user", "wtf")
                }
            },
            modifier = Modifier
                .width(250.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Регистрация")
        }
        BackHandler {
            onBackClicked()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginUser: () -> Unit,
    onLoginDispatcher : () -> Unit,
    onRegisterClicked:() -> Unit,
    modifier: Modifier = Modifier
) {
    var password by rememberSaveable { mutableStateOf("1234") }
    var phone by rememberSaveable { mutableStateOf("79771231488") }
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Диспетчерская служба",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(all = 12.dp)
                .fillMaxWidth()
        )
        TextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Номер телефона") }
        )
        Spacer(modifier = Modifier.height(10.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        Spacer(modifier = Modifier.padding(all=4.dp))
        Button(
            onClick = {
                Thread { //TODO GET VALUE FORM THREAD AND MAKE THREAD FOR ONLY FUNC, CLIENT/DISPATCHER SCREEN DOESNOT WORK!
                    val loginState =
                        DatabaseManager.loginUser(UserInfo(phone = phone, password = password))
                    if (loginState == "Password" || loginState == "None") {
                        Toast.makeText(
                            MainActivity.applicationContext(),
                            "Error: $loginState",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        MainActivity.userInfo = DatabaseManager.getUser(phone) ?: UserInfo()
                        when (MainActivity.userInfo.profession) {
                            "Client" -> {
                                MainActivity.userRequests =
                                    DatabaseManager.getUserRequests(MainActivity.userInfo)
                                onLoginUser()
                            }
                            "Dispatcher" -> {
                                onLoginDispatcher()
                            }
                        }
                    }
                }.start()
            },
            modifier = Modifier
                .width(250.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Вход")
        }
        Button(
            onClick = onRegisterClicked,
            modifier = Modifier
                .width(250.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Регистрация")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddingScreen(
    onAddClicked : (userRequest : UserRequest) -> Unit,
    onBackClicked : () -> Unit
) {
    var description by rememberSaveable { mutableStateOf("") }
    val accidentTypes = listOf("Холодное водоснабжение", "Горячее водоснабжение", "Газ", "Электричество", "Другое")
    val (selectedType, onTypeSelected) = rememberSaveable { mutableStateOf(accidentTypes[0]) }
    BackHandler() {
        onBackClicked()
    }
    Column (
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text (
            text = "Выберите тип аварии",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(all = 4.dp)
                .fillMaxWidth()
        )
        Column() {
            accidentTypes.forEach { text ->
                OutlinedButton(
                    onClick = { onTypeSelected(text) },
                    colors = if(selectedType == text) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    }
                ) {
                    Text(text = text)
                }
            }
        }
        Text (
            text = "Опишите ваш запрос",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 10.dp, bottom = 4.dp)
                .fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { if(it.length < 200) description = it },
            label = { Text("Опишите ваш запрос") },
            singleLine = false,
            maxLines = 3,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(start = 15.dp, top = 5.dp, end = 15.dp)
        )
        Button(
            onClick = {
                val userRequest = UserRequest(accidentType=selectedType, description = description)
                onAddClicked(userRequest)
            },
            modifier = Modifier.padding(top = 10.dp)
        ) {
            Text(text = "Отправить")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(onEditClick: () -> kotlin.Unit, onExitClick: () -> kotlin.Unit) {
    Column() {
        val userInfo = MainActivity.userInfo
        Text (
            text = "Информация о профиле",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(all = 4.dp)
                .fillMaxWidth()
        )
        var tmp_sn by rememberSaveable { mutableStateOf(userInfo.secondName) }
        TextField(
            value = tmp_sn,
            onValueChange = {
                tmp_sn = it
                userInfo.secondName = tmp_sn
            },
            label = { Text("Фамилия") },
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        var tmp_name by rememberSaveable { mutableStateOf(userInfo.firstName) }
        TextField(
            value = tmp_name,
            onValueChange = {
                tmp_name = it
                userInfo.firstName = tmp_name
            },
            label = { Text("Имя") },
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        var tmp_mn by rememberSaveable { mutableStateOf(userInfo.middleName) }
        TextField(
            value = tmp_mn,
            onValueChange = {
                tmp_mn = it
                userInfo.middleName = tmp_mn
            },
            label = { Text("Отчество") },
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        var tmp_phone by rememberSaveable { mutableStateOf(userInfo.phone) }
        TextField(
            value = tmp_phone,
            onValueChange = {
                tmp_phone = it
                userInfo.phone = tmp_phone
            },
            label = { Text("Номер телефона") },
            modifier = Modifier.padding(horizontal = 14.dp)
        )
        var tmp_pass by rememberSaveable { mutableStateOf(userInfo.password) }
        TextField(
            value = tmp_pass,
            onValueChange = {
                tmp_pass = it
                userInfo.password = tmp_pass
            },
            label = { Text("Пароль") },
            modifier = Modifier.padding(horizontal = 14.dp),
            visualTransformation = PasswordVisualTransformation()
        )
        if(userInfo.profession == "Client") {
            var tmp_address by rememberSaveable { mutableStateOf(userInfo.address) }
            TextField(
                value = tmp_address,
                onValueChange = {
                    tmp_address = it
                    userInfo.address = tmp_address
                },
                label = { Text("Адрес") },
                modifier = Modifier.padding(horizontal = 14.dp)
            )
        }
        Button(
            onClick = {
                MainActivity.userInfo = userInfo
                onEditClick()
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(all = 15.dp)
        ) {
            Text("Сохранить")
        }
        Button(
            onClick = {
                MainActivity.userInfo = UserInfo()
                MainActivity.userRequests = arrayListOf()
                MainActivity.dispatcherRequest = UserRequest()
                onExitClick()
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        ) {
            Text("Выйти из аккаунта")
        }
    }
}


@Preview(showBackground = true, widthDp = 320, heightDp = 500)
@Composable
fun DispatcherRequestScreenPreview() {
    DispatcherRequestScreen(onBackClicked = {}, onSaveClicked = {}, onDeleteClicked = {})
}

@Preview(showBackground = true, widthDp = 320, heightDp = 500)
@Composable
fun DispatcherRequestsScreenPreview() {
    DispatcherRequestsScreen(onViewClicked = {})
}

//
//@Preview(showBackground = true, widthDp = 320, heightDp = 500)
//@Composable
//fun LoginPreview() {
//    DispatcherServiceTheme {
//        LoginScreen(onContinueClicked = {})
//    }
//}
//
/*@Preview(showBackground = true, widthDp = 320)
@Composable
fun DefaultPreview() {
    DispatcherServiceTheme() {
        RequestsScreen(onClick = {})
    }
}*/