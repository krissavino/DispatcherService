package com.example.dispatcherservice

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.provider.ContactsContract.Data
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
    //Dispatcher
    DispatcherProfile,
    DispatcherRequest,
    DispatcherRequests
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
                || currentScreen == CurrentScreen.DispatcherProfile || currentScreen == CurrentScreen.DispatcherRequests) {
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
                            if(DatabaseManager.updateUser(MainActivity.userInfo))
                                MainActivity.userInfo = DatabaseManager.getUser(MainActivity.userInfo.phone) ?: UserInfo()
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
                CurrentScreen.DispatcherProfile -> {
                    DispatcherProfileScreen(onExitClick = {
                        currentScreen = CurrentScreen.Login
                    })
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

                else -> {

                }
            }
        }
    }
}

@Composable
fun DispatcherProfileScreen(onExitClick : () -> Unit) {
    Column() {
        val userInfo = MainActivity.userInfo
        Text (
            text = "Информация о пользователе",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(all = 4.dp)
                .fillMaxWidth()
        )
        Text (
            text = "Фамилия: ${userInfo.secondName}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp)
                .fillMaxWidth()
        )
        Text (
            text = "Имя: ${userInfo.firstName}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 14.dp, bottom = 10.dp)
                .fillMaxWidth()
        )
        Text (
            text = "Отчество: ${userInfo.middleName}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 14.dp, bottom = 10.dp)
                .fillMaxWidth()
        )
        Text (
            text = "Номер телефона: ${userInfo.phone}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 14.dp, bottom = 10.dp)
                .fillMaxWidth()
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = {
                    MainActivity.userInfo = UserInfo()
                    MainActivity.userRequests = arrayListOf()
                    MainActivity.dispatcherRequest = UserRequest()
                    onExitClick()
                }
            ) {
                Text("Выход")
            }
        }

    }
}
//TODO INSTALL DATABASE ON ANDROID!
@Composable
fun MenuButtons(onClick: (screenType : CurrentScreen) -> Unit) {
    var screenType = if(MainActivity.userInfo.profession == "Client")
        listOf(CurrentScreen.UserProfile, CurrentScreen.UserRequests)
    else
        listOf(CurrentScreen.DispatcherProfile, CurrentScreen.DispatcherRequests)
    var (selectedType, onSelectType) = rememberSaveable { mutableStateOf(screenType[1]) }
    Row(modifier = Modifier.fillMaxWidth()) {
        screenType.forEach { text ->
            Column( modifier = Modifier.weight(0.5f) ) {
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
                            CurrentScreen.DispatcherProfile -> "Профиль"
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

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun DispatcherRequestsScreen(onViewClicked: (request : UserRequest) -> Unit) {
    Column {
        Box(Modifier.fillMaxSize()) {
            DispatcherRequests(onViewClicked)
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
fun DispatcherRequestsFilter(filterClicked : (filter : String) -> Unit) {
    var filterExpanded by remember {mutableStateOf(false)}
    val filterTypes = listOf("Время", "Статус", "Тип аварии")
    var selectedType by remember { mutableStateOf(filterTypes[0]) }
    Box(modifier = Modifier
        .padding(16.dp)
        .fillMaxWidth()) {
        ExposedDropdownMenuBox(
            expanded = filterExpanded,
            onExpandedChange = { filterExpanded = !filterExpanded }) {
            TextField(value = selectedType, onValueChange = {}, readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = filterExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .align(Alignment.Center)
            )
            ExposedDropdownMenu(
                expanded = filterExpanded,
                onDismissRequest = { filterExpanded = false }
            ) {
                filterTypes.forEach { item ->
                    DropdownMenuItem(text = { Text(text = item) }, onClick = {
                        filterClicked(item)
                        selectedType = item
                        filterExpanded = false
                    })
                }
            }
        }
    }
}

private fun getDispatcherRequests() {
    val tmpPair = DatabaseManager.getDispatcherInfo()
    MainActivity.userRequests = tmpPair.first
    MainActivity.dispatcherUsers = tmpPair.second
}

@Composable
private fun DispatcherRequestsList(filterType : String, onViewClicked : (request : UserRequest) -> Unit) {
    var sortedRequests = MainActivity.userRequests.sortedBy { it.date }
    if(filterType == "Тип аварии") sortedRequests = MainActivity.userRequests.sortedBy { it.accidentType }
    if(filterType == "Статус") sortedRequests = MainActivity.userRequests.sortedBy { it.state }
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
            Thread { getDispatcherRequests() }.start()
            isRefreshing =  true
        }
    ) {
        LazyColumn() {
            items(sortedRequests) { item ->
                DispatcherRequestCard(item, onViewClicked)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DispatcherRequests(onViewClicked: (request : UserRequest) -> Unit) {
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
            var filterType by remember { mutableStateOf("Время") }
            DispatcherRequestsFilter(filterClicked = { filter ->
                filterType = filter
            })
            DispatcherRequestsList(filterType = filterType, onViewClicked)
        }
    }
}

@Composable
private fun UserRequests() {
    Column( ) {
        var userRequests by rememberSaveable { mutableStateOf(MainActivity.userRequests) }
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
                    MainActivity.userRequests = DatabaseManager.getUserRequests(MainActivity.userInfo)
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
                    Thread { MainActivity.userRequests = DatabaseManager.getUserRequests(MainActivity.userInfo) }.start()
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
        Text (
            text = "Фамилия: ${userInfo.secondName}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 14.dp, top = 10.dp, bottom = 10.dp)
                .fillMaxWidth()
        )
        Text (
            text = "Имя: ${userInfo.firstName}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 14.dp, bottom = 10.dp)
                .fillMaxWidth()
        )
        Text (
            text = "Отчество: ${userInfo.middleName}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 14.dp, bottom = 10.dp)
                .fillMaxWidth()
        )
        Text (
            text = "Номер телефона: ${userInfo.phone}",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 14.dp, bottom = 10.dp)
                .fillMaxWidth()
        )
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
        Button(
            onClick = onEditClick,
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
