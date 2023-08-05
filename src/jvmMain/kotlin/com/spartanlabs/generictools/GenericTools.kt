package com.spartanlabs.generictools

import java.awt.Desktop
import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import javax.imageio.ImageIO


fun String.capitalizeEveryWord() =
    split(' ').map{it.capitalize()} .toString().let {
        it.substring(1,it.length-1).replace(",","")
    }
fun cropImage(image:BufferedImage, x: Int,y: Int,width: Int,height: Int) =
    BufferedImage(width,height, BufferedImage.TYPE_INT_ARGB).apply{
        graphics.drawImage(image,0,0,width,height,x,y,x+width,y+height,null)
    }
fun saveImage(image:BufferedImage, filePath:String) = File("$filePath.png").apply{
    ImageIO.write(image, "png", this)
}
fun screenshotBrowser(address:String):BufferedImage{
    openInBrowser(address)
    Thread.sleep(1600)
    return screenshotArea(x = 40, y = 100, width = 2520, height = 1280)
}
fun openInBrowser(address:String) = Desktop.getDesktop().browse(URL(address).toURI())
fun screenshotArea(x:Int, y:Int, width:Int, height:Int) =
    screenshot(Rectangle().apply { this.x = x;this.y = y;this.width = width;this.height = height })
fun screenshotFull() =
    screenshot(Rectangle(Toolkit.getDefaultToolkit().screenSize))
private fun screenshot(area:Rectangle) = Robot().createScreenCapture(area)!!
private fun click(x:Int, y:Int){
    Robot().apply {
        mouseMove(x,y)
        mousePress(InputEvent.BUTTON1_DOWN_MASK)
        mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
    }
}
private fun typeKey(key:Int){
    Robot().apply{
        keyPress(key)
        keyRelease(key)
    }
}
private fun shiftType(key:Int){
    Robot().apply {
        keyPress(KeyEvent.VK_SHIFT)
        typeKey(key)
        keyRelease(KeyEvent.VK_SHIFT)
    }
}
private fun typeText(text:String){
    for(c in text){
        typeKey(c.code - 32)
    }
}
private fun typeCharacters(text:String){
    for(c in text){
        typeKey(c.code)
    }
}
private fun typeRandomKey() = typeKey((Math.random() * 26).toInt() + 65)
private fun typeRandomKeys(amount:Int){
    for (i in 1..amount)
        typeRandomKey()
}
fun main(){
    var x = 20
    var y = 20
    var screenShotAt = {
        saveImage(screenshotArea(x, y, 300, 300), "test")
    }
    while(true) {
        x = 20
        y = 20
        Thread.sleep(1100)
        openInBrowser("https://google.com")
        Thread.sleep(7000)
        click(x, y)
        Thread.sleep(500)
        x = 100
        y = 95
        click(x, y)

        //vpn 1
        Thread.sleep(550)
        x = 180
        y = 47
        //com.spartanlabs.generictools.click(x, y)
        Thread.sleep(200)
        x = 270
        y = 180
        //com.spartanlabs.generictools.click(x, y)
        //Adress bar
        x = 300
        y = 45
        click(x, y)
        typeText("https")
        shiftType(';'.code)
        typeKey('/'.code)
        typeKey('/'.code)
        typeText("easy")
        typeKey('-'.code)
        typeText("earn")
        typeKey('.'.code)
        typeText("homes")
        typeKey('/'.code)
        typeCharacters("392195692481")
        Thread.sleep(200)

        typeKey(KeyEvent.VK_ENTER)
        Thread.sleep(1200)

        click(3800, 20)
        Thread.sleep(400)
        click(3800, 20)
        continue

        Thread.sleep(5300)
        x = 800
        y = 530
        click(x, y)
        Thread.sleep(2600)
        x = 1250
        y = 700
        click(x, y)
        typeRandomKeys(8)
        click(x, y + 70)
        typeRandomKeys(8)
        click(x, y + 140)
        typeRandomKeys(16)
        shiftType(50)
        typeText("gmail")
        typeKey(46)
        typeText("com")
        var pass = ArrayList<Int>()
        for (c in 1..16)
            pass.add((Math.random() * 26).toInt() + 65)
        y += 210
        click(x, y)
        for (c in pass)
            typeKey(c)
        click(x, y + 70)
        for (c in pass)
            typeKey(c)
        x -= 160
        y += 110
        click(x, y)
        y += 100
        click(x, y)
        screenShotAt()
        Thread.sleep(3000)
        click(3800, 20)
        Thread.sleep(500)
        click(3800, 20)
        Thread.sleep(3000)
    }
}
