package com.dungeon.widget

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class InventoryActivity:Activity(){
    private lateinit var root:LinearLayout
    private lateinit var content:LinearLayout
    private lateinit var scroll:ScrollView
    private lateinit var tabs:LinearLayout
    private var h=HeroState()
    private var section=0
    private var sortMode=0
    private var filterSlot="all"
    private var filterRarity=-1
    private var focusedSlot="ring"
    private var filtersExpanded=false
    private val gold=0xFFEBC383.toInt()
    private val ivory=0xFFF1EFE9.toInt()
    private val faded=0xFFACBCCC.toInt()
    private val bg=0xFF111925.toInt()
    private fun dp(n:Int)=(resources.displayMetrics.density*n+0.5f).toInt()
    private fun round(fill:Int=0xFF202B39.toInt(),stroke:Int=0xFF475467.toInt(),
        radius:Int=13)=GradientDrawable().apply{
        setColor(fill);cornerRadius=dp(radius).toFloat();setStroke(dp(1),stroke)
    }
    private fun label(s:String,size:Float=14f,color:Int=ivory,strong:Boolean=false)=
        TextView(this).apply{
            text=s;textSize=size;setTextColor(color)
            if(strong)typeface=Typeface.create("sans-serif-medium",Typeface.BOLD)
        }
    private fun add(parent:LinearLayout,s:String,size:Float=14f,color:Int=ivory,
        strong:Boolean=false):TextView{
        val t=label(s,size,color,strong)
        parent.addView(t,LinearLayout.LayoutParams(-1,-2))
        return t
    }
    private fun gap(parent:LinearLayout,n:Int=8){
        parent.addView(View(this),LinearLayout.LayoutParams(1,dp(n)))
    }
    private fun row(parent:LinearLayout):LinearLayout{
        val r=LinearLayout(this).apply{
            orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL
        }
        parent.addView(r,LinearLayout.LayoutParams(-1,-2))
        return r
    }
    private fun button(parent:LinearLayout,title:String,weight:Float=0f,
        chosen:Boolean=false,onClick:()->Unit):TextView{
        val v=label(title,12f,if(chosen)gold else ivory,true).apply{
            gravity=Gravity.CENTER
            setPadding(dp(6),dp(11),dp(6),dp(11))
            background=round(if(chosen)0xFF3A4554.toInt() else 0xFF2C3747.toInt(),
                if(chosen)gold else 0xFF435163.toInt(),10)
            setOnClickListener{onClick()}
        }
        val params=if(weight>0f)LinearLayout.LayoutParams(0,-2,weight)
            else LinearLayout.LayoutParams(-1,-2)
        params.topMargin=dp(4);params.marginEnd=dp(4)
        parent.addView(v,params)
        return v
    }
    private fun card(title:String,body:LinearLayout.()->Unit){
        val pane=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;background=round()
            setPadding(dp(13),dp(12),dp(13),dp(12))
        }
        content.addView(pane,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(9)})
        add(pane,title,15f,gold,true);gap(pane,8)
        pane.body()
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        h=DungeonGame.advance(this)
        window.statusBarColor=bg;window.navigationBarColor=bg
        root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)
            setPadding(dp(12),dp(10),dp(12),dp(10))
        }
        root.setOnApplyWindowInsetsListener{_,insets->
            val sys=insets.getInsets(WindowInsets.Type.systemBars())
            root.setPadding(dp(12),sys.top+dp(10),dp(12),sys.bottom+dp(10))
            insets
        }
        val top=row(root)
        button(top,"‹ НАЗАД",1f){finish()}
        val title=label("▤  СНАРЯЖЕНИЕ",20f,gold,true).apply{
            gravity=Gravity.CENTER_VERTICAL
        }
        top.addView(title,LinearLayout.LayoutParams(0,-2,2f))
        gap(root,8)
        tabs=row(root)
        scroll=ScrollView(this)
        content=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL;setPadding(0,dp(12),0,dp(18))
        }
        scroll.addView(content)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root);draw()
    }
    override fun onResume(){
        super.onResume()
        if(::content.isInitialized){h=DungeonGame.advance(this);draw()}
    }
    private fun change(task:()->Boolean){
        if(!task())Toast.makeText(this,"Действие недоступно",Toast.LENGTH_SHORT).show()
        h=DungeonGame.advance(this);draw();DungeonWidgetProvider.refreshAll(this)
    }
    private fun select(sectionId:Int){
        section=sectionId;scroll.scrollTo(0,0);draw()
    }
    private fun draw(){
        h=DungeonGame.advance(this)
        tabs.removeAllViews()
        listOf("▤ РЮКЗАК","⛨ ЭКИПИРОВКА","⚒ МАТЕРИАЛЫ").forEachIndexed{idx,name->
            button(tabs,name,1f,section==idx){select(idx)}
        }
        val oldScroll=scroll.scrollY
        content.removeAllViews()
        when(section){0->bag();1->equipment();else->materials()}
        if(oldScroll>0)scroll.post{scroll.scrollTo(0,oldScroll)}
    }
    private fun bag(){
        if(h.gear.size>=h.inventoryLimit-10){
            card("♻ ОСВОБОДИТЬ МЕСТО"){
                add(this,"Игра сохранит надетые, избранные, непросмотренные и лучшие вещи каждого типа и комплекта.",12f,faded)
                val common=EquipmentLogic.duplicateCandidates(h,false)
                val all=EquipmentLogic.duplicateCandidates(h,true)
                button(this,"ОЧИСТИТЬ ДУБЛИКАТЫ · "+common.size+" (БЕЗ ЛЕГЕНДАРНЫХ)"){
                    confirmDuplicates(false)
                }
                button(this,"ПРОВЕРИТЬ И ЛЕГЕНДАРНЫЕ · "+all.size){
                    confirmDuplicates(true)
                }
                add(this,"Перед разбором появится список предметов и подтверждение.",11f,faded)
            }
        }
        card("ФИЛЬТРЫ И СОРТИРОВКА"){
            button(this,if(filtersExpanded)"▴ СВЕРНУТЬ ФИЛЬТРЫ" else "▾ ПОКАЗАТЬ ФИЛЬТРЫ"){
                filtersExpanded=!filtersExpanded
                draw()
            }
            if(filtersExpanded){
            val slots=listOf("all","weapon","armor","helmet","gloves","boots","ring","amulet")
            slots.chunked(4).forEach{group->
                val line=row(this)
                group.forEach{slot->
                    val name=when(slot){
                        "all"->"ВСЕ";"weapon"->"МЕЧИ";"armor"->"БРОНЯ"
                        "helmet"->"ШЛЕМ";"gloves"->"ПЕРЧ."
                        "boots"->"ОБУВЬ";"ring"->"КОЛЬЦА";else->"АМУЛЕТ"
                    }
                    button(line,name,1f,slot==filterSlot){filterSlot=slot;draw()}
                }
            }
            gap(this,5);add(this,"РЕДКОСТЬ",11f,faded,true)
            listOf(-1,0,1,2,3,4).chunked(3).forEach{group->
                val line=row(this)
                group.forEach{r->
                    button(line,if(r==-1)"ЛЮБАЯ" else "★".repeat(r+1),1f,filterRarity==r){
                        filterRarity=r;draw()
                    }
                }
            }
            gap(this,5)
            val sortLine=row(this)
            listOf("РЕДКОСТЬ","СИЛА","ТИП","ИЗБРАННОЕ").forEachIndexed{idx,name->
                button(sortLine,name,1f,sortMode==idx){sortMode=idx;draw()}
            }
            }
        }
        val items=h.gear.filter{
            (filterSlot=="all"||it.slot==filterSlot) &&
                (filterRarity==-1||it.rarity==filterRarity)
        }.let{list->when(sortMode){
            1->list.sortedByDescending{EquipmentLogic.weight(it,h.heroClass)}
            2->list.sortedWith(compareBy<Gear>{it.slot}.thenByDescending{it.rarity})
            3->list.sortedWith(compareByDescending<Gear>{it.id in h.favorites}
                .thenByDescending{it.rarity})
            else->list.sortedWith(compareByDescending<Gear>{it.rarity}
                .thenByDescending{it.power})
        }}
        card("ПРЕДМЕТЫ · "+items.size){
            if(items.isEmpty())add(this,"Нет предметов с такими фильтрами.",13f,faded)
            items.chunked(4).forEach{four->
                val line=row(this)
                for(item in four){
                    val tile=LinearLayout(this@InventoryActivity).apply{
                        orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
                        background=round(0xFF263244.toInt(),PixelGearIcon.rarityColor(item.rarity),9)
                        setPadding(dp(2),dp(5),dp(2),dp(5))
                        setOnClickListener{showItem(item.id)}
                    }
                    line.addView(tile,LinearLayout.LayoutParams(0,dp(106),1f).apply{
                        marginEnd=dp(4);bottomMargin=dp(4)
                    })
                    val img=ImageView(this@InventoryActivity).apply{
                        setImageBitmap(PixelGearIcon.create(item,54))
                    }
                    tile.addView(img,LinearLayout.LayoutParams(dp(54),dp(54)))
                    add(tile,"+"+item.power,12f,ivory,true).gravity=Gravity.CENTER
                    val badge=when{
                        item.id in h.equipment.values->"НАДЕТО"
                        item.id in h.favorites->"🔒 ИЗБР."
                        item.id in h.newItems->"НОВЫЙ"
                        item.set>0->"КОМПЛЕКТ"
                        EquipmentLogic.recommendation(h,item)>4->"↑ ЛУЧШЕ"
                        else->"★".repeat(item.rarity+1)
                    }
                    add(tile,badge,9f,PixelGearIcon.rarityColor(item.rarity),true).apply{
                        gravity=Gravity.CENTER;maxLines=1
                    }
                }
                repeat(4-four.size){
                    line.addView(View(this@InventoryActivity),
                        LinearLayout.LayoutParams(0,dp(106),1f))
                }
            }
        }
        card("⚒ БЫСТРЫЙ РАЗБОР"){
            add(this,"Надетые, избранные, эпические, легендарные и комплектные вещи защищены.",12f,faded)
            val buttons=row(this)
            button(buttons,"ДО ★★",1f){confirmBulk(1)}
            button(buttons,"ДО ★★★",1f){confirmBulk(2)}
            button(this,"НАДЕТЬ ЛУЧШЕЕ ДЛЯ "+h.className.uppercase()){
                change{DungeonGame.equipBest(this@InventoryActivity,h.heroClass)}
            }
        }
        card("⚙ АВТОМАТИЗАЦИЯ"){
            button(this,if(h.autoEquip)"✓ АВТОЭКИПИРОВКА ВКЛ" else "АВТОЭКИПИРОВКА ВЫКЛ"){
                change{DungeonGame.changeSettings(this@InventoryActivity,autoGear=!h.autoEquip)}
            }
            button(this,if(h.autoSalvage)"✓ УМНЫЙ АВТОРАЗБОР ВКЛ" else "УМНЫЙ АВТОРАЗБОР ВЫКЛ"){
                change{DungeonGame.changeSettings(this@InventoryActivity,auto=!h.autoSalvage)}
            }
            add(this,"Авторазбор предметов не выше:",12f,faded)
            val line=row(this)
            listOf(0,1,2).forEach{r->
                button(line,"★".repeat(r+1),1f,h.autoSalvageRarity==r){
                    change{DungeonGame.changeSettings(this@InventoryActivity,autoRarity=r)}
                }
            }
            add(this,"Улучшения подбираются автоматически, но избранные надетые вещи не заменяются.",11f,faded)
        }
    }
    private fun equipment(){
        card("⛨ ЭКИПИРОВКА · "+h.className.uppercase()){
            add(this,"⚔ "+h.attack+"  ⛨ "+h.defense+"  ♥ "+h.maxHp+
                "  ✦ "+h.crit+"% крит  ◇ "+h.dodge+"% уворот",14f,ivory,true)
            add(this,"Комплектный бонус +"+h.setBonus+
                " к атаке и защите. Регенерация +"+h.equipmentStats.regeneration,12f,faded)
            gap(this,7)
            listOf("ВОИН","СЛЕДОПЫТ","МАГ").forEachIndexed{role,name->
                button(this,(if(role==h.heroClass)"✓ " else "ПОДОБРАТЬ: ")+name){
                    change{DungeonGame.equipBest(this@InventoryActivity,role)}
                }
            }
        }
        card("СЛОТЫ СНАРЯЖЕНИЯ"){
            EquipmentLogic.slots.chunked(2).forEach{pair->
                val line=row(this)
                for(slot in pair){
                    val item=h.gear.firstOrNull{it.id==h.equipment[slot]}
                    val tile=LinearLayout(this@InventoryActivity).apply{
                        orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER
                        background=round(0xFF263244.toInt(),
                            if(item==null)0xFF566275.toInt() else PixelGearIcon.rarityColor(item.rarity))
                        setPadding(dp(4),dp(8),dp(4),dp(8))
                        setOnClickListener{
                            focusedSlot=slot
                            if(item!=null)showItem(item.id)
                            else{
                                section=0;filterSlot=if(slot=="ring2")"ring" else slot
                                scroll.scrollTo(0,0);draw()
                            }
                        }
                    }
                    line.addView(tile,LinearLayout.LayoutParams(0,dp(146),1f).apply{
                        marginEnd=dp(6);bottomMargin=dp(6)
                    })
                    add(tile,EquipmentLogic.icon(slot)+" "+EquipmentLogic.label(slot),12f,gold,true)
                        .gravity=Gravity.CENTER
                    if(item!=null){
                        tile.addView(ImageView(this@InventoryActivity).apply{
                            setImageBitmap(PixelGearIcon.create(item,55))
                        },LinearLayout.LayoutParams(dp(55),dp(55)))
                        add(tile,item.name,12f,ivory,true).apply{
                            gravity=Gravity.CENTER;maxLines=2
                        }
                        add(tile,"+"+item.power+" "+"★".repeat(item.rarity+1),10f,
                            PixelGearIcon.rarityColor(item.rarity)).gravity=Gravity.CENTER
                    }else{
                        gap(tile,16);add(tile,"+ ВЫБРАТЬ",12f,faded).gravity=Gravity.CENTER
                    }
                }
            }
        }
        card("◈ КОМПЛЕКТЫ"){
            add(this,"2 предмета комплекта: +4 к атаке/защите."+
                "\n4 предмета: +12 к атаке/защите и +24 HP.",12f,faded)
            val active=EquipmentLogic.equipped(h).filter{it.set>0}.groupingBy{it.set}.eachCount()
            if(active.isEmpty())add(this,"Комплекты ещё не собраны.",12f,faded)
            else active.forEach{(set,count)->add(this,"Комплект "+set+": "+count+" предметов",13f,gold)}
        }
    }
    private fun materials(){
        card("⚒ МАТЕРИАЛЫ"){
            add(this,"✦ Материалы: "+h.shards,18f,gold,true);gap(this,5)
            add(this,"◈ Кристаллы: "+h.gems,16f,ivory,true);gap(this,5)
            add(this,"◉ Золото: "+h.gold,16f,ivory,true);gap(this,7)
            add(this,"Материалы получаются при разборе экипировки."+
                "\nКристаллы — из боссов, реликвий и заданий.",12f,faded)
        }
        card("ПОСЛЕДНИЕ НАХОДКИ"){
            if(h.lootHistory.isEmpty())add(this,"История появится после новых находок.",12f,faded)
            else h.lootHistory.take(10).forEach{
                add(this,it,12f,faded);gap(this,5)
            }
        }
        card("РЕЦЕПТЫ КУЗНИЦЫ"){
            add(this,"Новый предмет · "+(5+h.forge*2)+" материалов, "+
                (30+h.forge*12)+" золота",13f,ivory)
            button(this,"⚒ ВЫКОВАТЬ"){
                change{DungeonGame.forgeItem(this@InventoryActivity)}
            }
            gap(this,8)
            add(this,"Усилить оружие · "+(4+h.forge)+" материалов, "+
                (20+h.level*3)+" золота",13f,ivory)
            button(this,"⚔ УЛУЧШИТЬ ОРУЖИЕ"){
                change{DungeonGame.improveWeapon(this@InventoryActivity)}
            }
        }
        card("РЕДКОСТЬ ПРЕДМЕТОВ"){
            listOf("Обычная","Необычная","Редкая","Эпическая","Легендарная").forEachIndexed{idx,name->
                add(this,"★".repeat(idx+1)+"  "+name,14f,PixelGearIcon.rarityColor(idx),true)
                gap(this,5)
            }
            add(this,"Эпические, легендарные и комплектные вещи автоматически защищены.",12f,faded)
        }
    }
    private fun confirmDuplicates(includeLegendary:Boolean){
        val candidates=EquipmentLogic.duplicateCandidates(h,includeLegendary)
        if(candidates.isEmpty()){
            Toast.makeText(this,"Лишних дубликатов нет",Toast.LENGTH_SHORT).show()
            return
        }
        val materials=candidates.sumOf{1+it.rarity*2}
        val examples=candidates.take(7).joinToString("\\n"){item->
            "• "+item.name+" · "+"★".repeat(item.rarity+1)+" · +"+item.power
        }
        val extra=if(candidates.size>7)"\\n… и ещё "+(candidates.size-7)+" предметов" else ""
        val warning=if(includeLegendary)
            "\\nВНИМАНИЕ: среди них могут быть легендарные предметы!" else
            "\\nЛегендарные предметы не затрагиваются."
        AlertDialog.Builder(this)
            .setTitle("Разобрать "+candidates.size+" дубликатов?")
            .setMessage(examples+extra+"\\n\\nМатериалы: +"+materials+warning+
                "\\nНадетые, избранные и новые находки остаются. Отменить разбор нельзя.")
            .setPositiveButton("РАЗОБРАТЬ "+candidates.size){_,_->
                change{DungeonGame.salvageDuplicates(this@InventoryActivity,includeLegendary)}
            }.setNegativeButton("ОТМЕНА",null).show()
    }
    private fun confirmBulk(maxRarity:Int){
        val items=h.gear.filter{it.rarity<=maxRarity&&EquipmentLogic.canSalvage(h,it)}
            .filter{filterSlot=="all"||it.slot==filterSlot}
        if(items.isEmpty()){
            Toast.makeText(this,"Подходящих предметов нет",Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this).setTitle("Разобрать "+items.size+" предметов?")
            .setMessage("Надетые и защищённые предметы не затрагиваются."+
                "\nМатериалы: +"+items.sumOf{1+it.rarity*2}+". Отменить нельзя.")
            .setPositiveButton("РАЗОБРАТЬ"){_,_->
                change{DungeonGame.salvageFiltered(this,maxRarity,filterSlot)}
            }.setNegativeButton("Отмена",null).show()
    }
    private fun showItem(id:String){
        DungeonGame.viewItem(this,id)
        h=DungeonGame.advance(this)
        val g=h.gear.firstOrNull{it.id==id}?:return
        val second=g.slot=="ring"&&focusedSlot=="ring2"
        val delta=EquipmentLogic.preview(h,g,second)
        val current=h.gear.firstOrNull{
            it.id==h.equipment[EquipmentLogic.targetSlot(g,second)]
        }
        val pane=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(8),dp(16),dp(10))
        }
        pane.addView(ImageView(this).apply{
            setImageBitmap(PixelGearIcon.create(g,108))
        },LinearLayout.LayoutParams(-1,dp(108)))
        gap(pane,5)
        add(pane,g.name,18f,PixelGearIcon.rarityColor(g.rarity),true)
        add(pane,DungeonGame.rarityName(g.rarity)+" · "+EquipmentLogic.label(g.slot)+
            " · "+"★".repeat(g.rarity+1),13f,faded)
        gap(pane,6)
        add(pane,"Сила +"+g.power+" · "+g.trait+
            (if(g.set>0)" · комплект "+g.set else ""),13f,ivory)
        gap(pane,7)
        add(pane,"СРАВНЕНИЕ С НАДЕТЫМ",13f,gold,true)
        add(pane,"Текущее: "+(current?.name?:"пустой слот"),12f,faded)
        val score=EquipmentLogic.improvement(h,g,EquipmentLogic.targetSlot(g,second))
        val suggestion=when{
            g.id in h.equipment.values->"✓ Сейчас надето"
            score>4->"↑ РЕКОМЕНДУЕТСЯ НАДЕТЬ"
            score<0->"↓ Текущая экипировка лучше"
            else->"≈ Похожая эффективность"
        }
        add(pane,suggestion,13f,if(score>4)0xFF86DAA7.toInt() else faded,true)
        fun metric(name:String,value:Int){
            val displayed=if(value>0)"+"+value else ""+value
            val color=when{value>0->0xFF87DCA5.toInt();value<0->0xFFF18F93.toInt();else->faded}
            add(pane,name+": "+displayed,13f,color,value!=0)
        }
        metric("⚔ Атака",delta.attack);metric("⛨ Защита",delta.defense)
        metric("♥ HP",delta.health);metric("✦ Крит",delta.crit)
        metric("◇ Уворот",delta.dodge)
        metric("✚ Регенерация",delta.regeneration)
        metric("☠ Яд",delta.poison)
        if(delta.setBonus!=0)metric("Комплект",delta.setBonus)
        gap(pane,5)
        val scrollPane=ScrollView(this).apply{
            isFillViewport=false
            addView(pane)
        }
        val dialog=AlertDialog.Builder(this).setView(scrollPane)
            .setNegativeButton("ЗАКРЫТЬ",null).create()
        if(g.id!=h.equipment[g.slot])button(pane,"✓ НАДЕТЬ"){
            dialog.dismiss();change{DungeonGame.equip(this,id)}
        }
        if(g.slot=="ring"&&g.id!=h.equipment["ring2"])button(pane,"✓ НАДЕТЬ ВО ВТОРОЙ СЛОТ"){
            dialog.dismiss();change{DungeonGame.equip(this,id,true)}
        }
        button(pane,if(id in h.favorites)"🔒 УБРАТЬ ИЗ ИЗБРАННОГО" else "🔒 ЗАЩИТИТЬ / В ИЗБРАННОЕ"){
            dialog.dismiss();change{DungeonGame.toggleFavorite(this,id)}
        }
        if(g.id !in h.equipment.values){
            button(pane,"⚒ РАЗОБРАТЬ ПРЕДМЕТ"){
                dialog.dismiss()
                AlertDialog.Builder(this).setTitle("Разобрать "+g.name+"?")
                    .setMessage("Предмет уничтожится навсегда. Материалы: +"+
                        (1+g.rarity*2)+
                        (if(EquipmentLogic.isProtected(h,g))
                            "\nВНИМАНИЕ: это защищённый или редкий предмет!" else ""))
                    .setPositiveButton("РАЗОБРАТЬ"){_,_->
                        change{DungeonGame.salvage(this,id,true)}
                    }.setNegativeButton("Отмена",null).show()
            }
        }else add(pane,"✓ ПРЕДМЕТ НАДЕТ",12f,0xFF80D19F.toInt(),true)
        dialog.show()
        dialog.setOnDismissListener{draw()}
    }
}
