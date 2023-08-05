
import com.spartanlabs.bottools.botactions.*
import com.spartanlabs.bottools.commands.GameStatsCommand
import com.spartanlabs.bottools.commands.Option
import com.spartanlabs.bottools.commands.SubCommand
import com.spartanlabs.bottools.dataprocessing.BaseXMLReader
import com.spartanlabs.bottools.dataprocessing.children
import com.spartanlabs.bottools.dataprocessing.getChild
import com.spartanlabs.bottools.main.Bot
import com.spartanlabs.generictools.capitalizeEveryWord
import com.spartanlabs.generictools.cropImage
import com.spartanlabs.generictools.saveImage
import com.spartanlabs.generictools.screenshotBrowser

class PalOfExile() : GameStatsCommand("poe") {
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
        SCPriceItem("currency","currency")
        getShardNames()
        SCPriceItem("shart","currency")
        getOilNames()
        SCPriceItem("oils","oils")
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

    private inner class SCPriceItem(searchName:String, val ninjaName:String): SubCommand(searchName,this@PalOfExile) {
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
            reply("Looking up the item: $itemName from the category: $name")
            val inSiteName = itemName.replace(" ","-")
            val screenshot = screenshotBrowser("https://poe.ninja/challenge/$ninjaName/$inSiteName")
            val crop = cropImage(screenshot, 832, 205, 1050, 868)
            val imageFile = saveImage(crop, "src/jvmTest/resources/screenshot")
            Bot send imageFile in channel
        }
    }

    private fun wikiItem(itemName:Array<String>){
        val itemName = getOption("itemname")?.asString?.capitalizeEveryWord()?.replace(' ','_')
        if(itemName.isNullOrBlank())reply("Could not find the specified item")
        val address = "https://www.poewiki.net/wiki/$itemName"
        open(address){
            reply(data)
        }
    }
    private fun getCurrencyNames() = acquireFromWiki("Currency#Basic_currency",::basicCurrency)
    private fun getShardNames() = acquireFromWiki("Currency#Basic_currency",::shardCurrency)
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
            currencyNames.add(valueMap["currencyitems"]!!
                .lowercase().replace("'",""))
        }
    }
    private fun shardCurrency(){
        val keyName = "currencyitems"
        while (true) {
            mapValueByKey(keyName)
            if (valueMap[keyName]!!.contains("Shard"))
                currencyNames.add(
                    valueMap[keyName]!!
                        .lowercase().replace("'", "")
                )
            if(!valueMap[keyName]!![0].isLetter())
                break
        }
    }
    private fun acquireFromWiki(wikipage:String, itemSearchFun:()->Unit) {
        currencyNames.clear()
        keyParser setDocument "src/jvmTest/resources/PoECurrencyKeys"
        try {
            open("https://www.poewiki.net/wiki/$wikipage") {
                itemSearchFun.invoke()
            }
        } catch (e: Exception) {
        }
    }
    private fun writeGroup(groupName:String){
        val reader = BaseXMLReader()
        reader setDocument "src/jvmTest/resources/PoECurrencyGroups.xml"
        val root = reader.root
        reader.writeItemsList(root, groupName, currencyNames)
    }
}
