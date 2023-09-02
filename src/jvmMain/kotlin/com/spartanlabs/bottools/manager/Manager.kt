package com.spartanlabs.bottools.manager

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.spartanlabs.bottools.main.Bot
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.runAsync


val log = LoggerFactory.getLogger("Manager")
lateinit var viewModel: ViewModel
fun start(bot: Lazy<Bot>){
    application{
        viewModel = ViewModel(bot)
        Window(onCloseRequest = ::exitApplication, title = "Trump Bot"){
            BotUI()
        }
    }
}
@Composable
private fun BotUI(){
    var vm = remember { viewModel }
    var statusText = vm.generalState
    MaterialTheme {
        Column(Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(10.dp))
            StartButton(Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(5.dp))
            Text("Current status: ${vm.generalState}",Modifier.align(Alignment.CenterHorizontally))
            if (vm.generalState!="not started"){
                //stateField(bot)
                //EventsBox(bot)
            }
        }

    }
}
@OptIn(ExperimentalUnitApi::class)
@Composable
private fun StartButton(modifier: Modifier) {
    var buttonText by remember{ mutableStateOf("Start!") }
    var vm = remember { viewModel }
    Button(
        onClick = {
            buttonText =  "Running!"
            runAsync{
                Bot.start()
                vm.bot
            }
        },
        modifier = modifier.height(60.dp).width(250.dp)
    ) {
        Column{
            Text(
                text = buttonText,
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
