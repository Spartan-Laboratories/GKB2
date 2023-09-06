package com.spartanlabs.bottools.manager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Brush.Companion.linearGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.spartanlabs.bottools.main.Bot
import java.util.concurrent.CompletableFuture.runAsync

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
    MaterialTheme{Row{
        Column(Modifier.fillMaxHeight().fillMaxWidth().weight(.22F)) {
            Spacer(modifier = Modifier.height(10.dp))
            StartButton()
            Spacer(modifier = Modifier.height(10.dp))
            if (vm.generalState!="not started"){
                Text(vm.log,Modifier.verticalScroll(rememberScrollState()))
            }
        }
        if(vm.generalState!="not started"){ Box(
            modifier = Modifier.fillMaxSize()
                .background(brush = rwbGrad())
                .padding(8.dp)
                .border(color = Color.Green, width = 4.dp, shape = RectangleShape)
                .weight(.78F),
            contentAlignment = Alignment.TopStart)
            {
                LazyColumn(Modifier.fillMaxHeight().fillMaxWidth().padding(7.dp)) {
                    items(5){
                        Text(it.toString())
                    }
                }
        } }
    }}
}

@Composable
private fun rwbGrad():Brush{
    return linearGradient(
        colors = listOf(
            Color.Red,
            Color.White,
            Color.Blue
        ),
        start = Offset(0f, 0f),
        end = Offset.Infinite
    )
}
@OptIn(ExperimentalUnitApi::class)
@Composable
private fun StartButton() {
    var buttonText by remember{ mutableStateOf("Start!") }
    var vm = remember { viewModel }
    var alignment by remember{ mutableStateOf(Alignment.TopCenter) }
    Box(Modifier.fillMaxWidth().height(100.dp),alignment, ) {
        Button(
            onClick = {
                buttonText = "Running!"
                alignment = Alignment.TopStart
                runAsync {
                    Bot.start()
                    vm.bot
                }
            },
            modifier = Modifier.height(100.dp).width(250.dp)
        ) {
            Column {
                Text(
                    text = buttonText,
                    fontSize = 25.sp,
                    fontWeight = FontWeight(10),
                    modifier = Modifier.fillMaxSize().weight(2F).align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
                Text(
                    "Current status: ${vm.generalState}",
                    Modifier.align(Alignment.CenterHorizontally),
                    fontSize = TextUnit(12F, TextUnitType.Sp)
                )
            }
        }
    }
}
