package com.spartanlabs.bot.commands
import com.spartanlabs.bottools.commands.GameStatsCommand
import com.spartanlabs.bottools.commands.Option
import com.spartanlabs.bottools.commands.SubCommand
import com.spartanlabs.bottools.dataprocessing.BaseXMLReader
import com.spartanlabs.bottools.dataprocessing.children
import com.spartanlabs.bottools.dataprocessing.getChild
import com.spartanlabs.generaltools.capitalizeEveryWord
import com.spartanlabs.generaltools.cropImage
import com.spartanlabs.webtools.WebViewer
import com.spartanlabs.generaltools.to
import java.lang.IndexOutOfBoundsException
private val webviewer = WebViewer()
class PalOfExile() : GameStatsCommand("poe", "https://poe.ninja/challenge/currency") {
    //override var brief = "Your best pal in all of Oriath"
    //override var details = "Helps find Path of Exile information"
    override fun invoke(args: Array<String>) {}
    private val price   = this + "price"
    private val wiki    = this + "wiki"
    private val currencyNames = ArrayList<String>()
    init{
        //MethodCommand(::wikiItem, "wikiitem", "provides a description of the item", this)+
        //        Option(name = "itemname", type = "string", description = "which item do you want to search for?", required = true)
        //wiki + orgData("item", "wikiitem")

        getCurrencyNames()
        SCPriceItem("currency", "currency")
        getShardNames()
        SCPriceItem("shart", "currency")
        getOilNames()
        SCPriceItem("oils", "oils")
        price and "currency" becomes "currency"
        price and "shart" becomes "shart"
        price and "oils" becomes "oils"
        /*
        val `acquire currency names` = "acquirecurrencynames"
        MethodCommand(::acquireCurrencyNames, `acquire currency names`, "$`acquire currency names` description", this)
        test and `acquire currency names` becomes `acquire currency names`
        */
        makeInteractive()
    }

    private inner class SCPriceItem(searchName:String, val ninjaGroupingName:String): SubCommand(searchName,this@PalOfExile) {
        override val brief      = "get the price of a specific $searchName"
        override val details    = "TODO: write detailed description here"
        private final val optionName = "${name}type"
        val specificItem = Option("string", optionName, "the name of the item that you want to look up", true)
        init{
            for(index in 0..Math.min(currencyNames.size - 1, 24))
                currencyNames[index].let { specificItem.addChoice(it,it) }
            addOption(specificItem)
        }
        override fun invoke(args:Array<String>){
            val itemName = getOption(optionName)!!.asString
            reply > "Looking up the item: $itemName from the category: $name"
            val ninjaCurrencyName = itemName.replace(" ","-")
            val url = "https://poe.ninja/challenge/$ninjaGroupingName/$ninjaCurrencyName"
            val screenshot = webviewer screenshot url
            val categorySize:CategorySize = when(name){
                "currency","shard"  -> CategorySize.BUYSELL
                else                -> CategorySize.BUYONLY
            }
            val crop = cropImage(screenshot, 145, 285, categorySize.width, categorySize.height)
            val imageFile = crop to "src/jvmTest/resources/screenshot"
            channel > imageFile
        }
    }
    private enum class CategorySize(val width:Int, val height:Int){

        BUYSELL(CategorySize.defaultWidth,320),
        BUYONLY(CategorySize.defaultWidth, CategorySize.defaultHeight);
        companion object {
            private const val defaultWidth = 330
            private const val defaultHeight = 254
        }
    }

    private fun wikiItem(itemName:Array<String>){
        val itemName = getOption("itemname")?.asString?.capitalizeEveryWord()?.replace(' ','_')
        if(itemName.isNullOrBlank())`reply with`("Could not find the specified item")
        val address = "https://www.poewiki.net/wiki/$itemName"
        open(address){
            `reply with`(data)
        }
    }
    private fun getCurrencyNames() = acquireFromWiki("Currency#Basic_currency",::basicCurrency)
    private fun getShardNames() = currencyNames.clear().also{
        arrayOf("mirror","fracturing","exalted","annulment").forEach{
                shardName->currencyNames.add("$shardName shard")
        }
    }
    //acquireFromWiki("Currency#Basic_currency",::shardCurrency)
    private fun getOilNames(){
        currencyNames.clear()
        val reader = BaseXMLReader()
        reader setDocument "src/jvmTest/resources/PoECurrencyGroups.xml"
        reader.root.getChild("oils")!!.children().forEach {
            currencyNames.add("${reader.getValue(it)} oil")
        }
    }
    private fun basicCurrency(){
        val keyName = "currencyitems"
        while (true) {
            mapValueByKey(keyName)
            val currentItem = valueMap[keyName]
            if(currentItem == "Chaos Orb")
                continue
            if(currentItem!!.contains("Shard"))
                break
            currencyNames.add(valueMap[keyName]!!
                .lowercase().replace("'",""))
        }
    }
    private fun shardCurrency(){
        val keyName = "currencyitems"
        while (true) {
            try{mapValueByKey(keyName)}catch (_:Exception){return}
            if (valueMap[keyName]!!.contains("Shard"))
                currencyNames.add(
                    valueMap[keyName]!!
                        .lowercase().replace("'", "")
                )
            try{
                if(valueMap.let { it.isEmpty() or (it[keyName]?.isEmpty() == true) or !it[keyName]!![0].isLetter() })
                    break
            }catch (e:IndexOutOfBoundsException){break}
        }
    }
    private fun acquireFromWiki(wikipage:String, itemSearchFun:()->Unit) {
        currencyNames.clear()
        keyParser setDocument "src/jvmTest/resources/PoECurrencyKeys"
        open("https://www.poewiki.net/wiki/$wikipage") {
            itemSearchFun.invoke()
        }
    }
    private fun writeGroup(groupName:String){
        val reader = BaseXMLReader()
        reader setDocument "src/jvmTest/resources/PoECurrencyGroups.xml"
        val root = reader.root
        reader.writeItemsList(root, groupName, currencyNames)
    }
}
