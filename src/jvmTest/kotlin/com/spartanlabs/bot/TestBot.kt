package com.spartanlabs.bot
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.spartanlabs.bottools.main.Bot
import com.spartanlabs.bottools.manager.start
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture.runAsync

/*
fun main() = application{
    //Window(onCloseRequest=::exitApplication){}
    Window(onCloseRequest = ::exitApplication, title = "Trump Bot"){
        BotUI()
    }
}

 */
fun main() {
    val bot = lazy {
        KotBot()
    }
    start(bot)
}
val log = LoggerFactory.getLogger("UI")
@OptIn(ExperimentalUnitApi::class)
@Composable
private fun BotUI(){
    var statusText by remember{mutableStateOf("Not started")}
    var initializerText by remember{mutableStateOf("Start!")}
    var readyTokenState = remember{mutableStateOf(false)}
    //var bot by remember{ mutableStateOf(lazy{ KotBot(readyTokenState) })}
    //var eventsText by remember{ mutableStateOf(lazy { bot.value.eventsText }) }
    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            Text(statusText)
            Spacer(modifier = Modifier.height(10.dp))
            //StartButton(Modifier.align(Alignment.CenterHorizontally))
        }
        Spacer(Modifier.width(1.dp))
        if (readyTokenState.value){
            Thread.sleep(1000L)
            //val bot = bot.value
            //stateField(bot)
            //EventsBox(bot)
        }
    }
}
/*
@OptIn(ExperimentalUnitApi::class)
@Composable
private fun StartButton(modifier: Modifier) {
    Button(
        onClick = {
            statusText = "Please wait, initializing!"
            initializerText = statusText
            runAsync{
                bot.value
            }
            botStarted = true
        },
        modifier = modifier.height(60.dp).width(250.dp)
    ) {
        Column{
            Text(
                text = initializerText,
                fontSize = 25.sp,
                fontWeight = FontWeight(10),
                modifier = Modifier.fillMaxSize().weight(2F).align(Alignment.CenterHorizontally),
                textAlign = TextAlign.Center
            )
            Text(
                "note: application will lag on start",
                fontSize = TextUnit(12F, TextUnitType.Sp),
                fontWeight = FontWeight(1),
                modifier = Modifier.fillMaxSize().weight(1F)
            )
        }

    }
}
@Composable
private fun EventsBox(bot:Bot!){
    Box(Modifier.fillMaxSize()){
        Text(eventsText.value)
    }
}

 */
@Composable
private fun stateField(bot:Bot){
    Column {
        Row{
            Spacer(Modifier.width(40.dp))
            commandsField(bot)
        }
        dateField(bot.centralProcess?.currentDate ?: "not started")
    }
}
@Composable
private fun commandsField(bot:Bot){
    var uptime by remember { bot.formattedUptime}
    var commandList by remember { mutableStateOf(Bot.commands) }
    var commandNameListSize by remember { mutableStateOf(commandList.size) }
    var commandNames by remember { mutableStateOf(commandList.keys.toList()) }
    if(commandNameListSize > 0)Box(
        modifier = Modifier.border(width = 5.dp, color = Color.Black, shape = RectangleShape),
    ) {
        Column(Modifier.padding(2.dp)){
            Spacer(Modifier.height(2.dp))
            Text("Command name list size: $commandNameListSize")
            Spacer(Modifier.height(3.dp))
            LazyColumn (modifier = Modifier.fillMaxSize().padding(10.dp)){
                items(commandNameListSize) { index ->
                    Text(index.toString())
                    commandField(commandNames[index])
                }
            }
        }
    }
}
@Composable
private fun commandField(commandName:String){
    var buttonColor by remember { mutableStateOf(Color.Green) }
    Button( border = BorderStroke(4.dp, Color.Green),
            onClick = {
                Bot.commandActiveStatus[commandName] = !Bot.commandActiveStatus[commandName]!!
                when(Bot.commandActiveStatus[commandName]){
                    false   -> buttonColor = Color.Red
                    true    -> buttonColor = Color.Green
                    else    -> buttonColor = Color.Black
                }
            }
    ){
        Text(commandName)
    }
}
@Composable
private fun dateField(date:String){
    val date by remember { mutableStateOf(date) }
    Text(date)
}
class KotBot : Bot() {
    override fun applyDailyUpdate(currentDate: String?) {
    }

}