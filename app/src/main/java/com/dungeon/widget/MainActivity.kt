package com.dungeon.widget

import android.app.Activity
import android.app.Dialog
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.BitmapDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity:Activity(){
    private val handler=Handler(Looper.getMainLooper())
    private lateinit var root:LinearLayout
    private lateinit var header:TextView
    private lateinit var headline:TextView
    private lateinit var scene:ImageView
    private lateinit var vitality:TextView
    private lateinit var narration:TextView
    private lateinit var tabs:LinearLayout
    private lateinit var body:LinearLayout
    private lateinit var details:ScrollView
    private var page=0
    private var sortMode=0
    private var frame=0
    private var lastUpdate=0L
    private var lastWidgetRefresh=0L
    private var lastPageRefresh=0L
    private var state=HeroState()
    private var running=false
    private var roomEnteredAt=System.currentTimeMillis()
    private var fullscreenDialog:Dialog?=null
    private var fullscreenView:ImageView?=null
    private var fullscreenLabel:TextView?=null
    private fun dp(n:Int)=(n*resources.displayMetrics.density+0.5f).toInt()
    private val gold get()=Color.rgb(239,197,124)
    private val muted get()=Color.rgb(160,172,187)
    private val ivory get()=Color.rgb(237,234,225)
    private val panel get()=when(state.theme){1->0xFF0F1926.toInt();2->0xFF1A2C2B.toInt();else->0xFF202938.toInt()}
    private val base get()=when(state.theme){1->0xFF0B1019.toInt();2->0xFF102322.toInt();else->0xFF111925.toInt()}
    private fun background(bg:Int=panel,stroke:Int=0xFF3D4A5A.toInt(),radius:Int=16)=GradientDrawable().apply{
        setColor(bg);cornerRadius=dp(radius).toFloat();setStroke(dp(1),stroke)
    }
    private fun text(t:String,size:Float=14f,shade:Int=ivory,bold:Boolean=false):TextView{
        return TextView(this).apply{
            this.text=t;textSize=size;setTextColor(shade)
            if(bold)typeface=Typeface.create("sans-serif-medium",Typeface.BOLD)
            setPadding(dp(3),dp(3),dp(3),dp(3))
        }
    }
    private fun addLine(parent:LinearLayout,t:String,size:Float=14f,shade:Int=ivory,bold:Boolean=false):TextView{
        val v=text(t,size,shade,bold);parent.addView(v);return v
    }
    private fun gap(parent:LinearLayout,n:Int=8){
        parent.addView(View(this),LinearLayout.LayoutParams(1,dp(n)))
    }
    private fun card(parent:LinearLayout,title:String,bodyText:String="",content:(LinearLayout.()->Unit)?=null):LinearLayout{
        val c=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            background=background()
            setPadding(dp(13),dp(12),dp(13),dp(12))
        }
        parent.addView(c,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(10)})
        addLine(c,title,15f,gold,true)
        if(bodyText.isNotBlank())addLine(c,bodyText,13f,muted)
        if(content!=null){gap(c,6);c.content()}
        return c
    }
    private fun action(parent:LinearLayout,label:String,enabled:Boolean=true,tint:Boolean=false,fn:()->Unit):TextView{
        val v=text(label,14f,if(enabled)ivory else muted,true).apply{
            gravity=Gravity.CENTER
            setPadding(dp(12),dp(12),dp(12),dp(12))
            background=background(if(tint)0xFF48543C.toInt() else 0xFF2B3646.toInt(),
                if(tint)0xFF8A9B66.toInt() else 0xFF47576B.toInt(),12)
            alpha=if(enabled)1f else 0.5f
            if(enabled)setOnClickListener{fn()}
        }
        parent.addView(v,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(5)})
        return v
    }
    private fun row(parent:LinearLayout):LinearLayout{
        val l=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        parent.addView(l,LinearLayout.LayoutParams(-1,-2));return l
    }
    private fun mini(parent:LinearLayout,label:String,fn:()->Unit){
        val v=text(label,12f,ivory,true).apply{
            gravity=Gravity.CENTER
            setPadding(dp(6),dp(10),dp(6),dp(10))
            background=background(0xFF2B3646.toInt(),0xFF465269.toInt(),9)
            setOnClickListener{fn()}
        }
        parent.addView(v,LinearLayout.LayoutParams(0,-2,1f).apply{marginEnd=dp(5)})
    }
    private val animator=object:Runnable{
        override fun run(){
            if(!running||isFinishing)return
            val now=System.currentTimeMillis()
            if(now-lastUpdate>=950L){
                val beforeFloor=state.floor
                val beforeRoom=state.room
                state=DungeonGame.advance(this@MainActivity)
                if(beforeFloor!=state.floor||beforeRoom!=state.room)roomEnteredAt=now
                lastUpdate=now
                updateNumbers()
                if(now-lastPageRefresh>=9000L)refreshPage()
            }
            val age=(now-roomEnteredAt).coerceAtLeast(0).coerceAtMost(2000).toInt()
            if(fullscreenDialog?.isShowing==true){
                fullscreenView?.setImageDrawable(BitmapDrawable(resources,
                    DungeonScene.render(state,frame++,true,1.58f,age)).apply{isFilterBitmap=false})
                fullscreenLabel?.text="♥ "+state.hp+"/"+state.maxHp+
                    "   Уровень "+state.level+"   ·   Этаж "+state.floor+
                    "\n"+state.log
            }else if(page==0){
                scene.setImageDrawable(BitmapDrawable(resources,
                    DungeonScene.render(state,frame++,true,1.32f,age)).apply{isFilterBitmap=false})
            }
            if(now-lastWidgetRefresh>=12000L){
                DungeonWidgetProvider.refreshAll(this@MainActivity)
                lastWidgetRefresh=now
            }
            handler.postDelayed(this,if(state.eco)280L else 90L)
        }
    }
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        state=DungeonGame.advance(this)
        roomEnteredAt=System.currentTimeMillis()
        window.statusBarColor=base;window.navigationBarColor=base
        window.decorView.systemUiVisibility=0
        root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(13),dp(8),dp(13),dp(10))
            setBackgroundColor(base)
        }
        root.setOnApplyWindowInsetsListener{_,insets->
            val sb=insets.getInsets(WindowInsets.Type.statusBars())
            val nb=insets.getInsets(WindowInsets.Type.navigationBars())
            root.setPadding(dp(13),dp(8)+sb.top,dp(13),dp(10)+nb.bottom)
            insets
        }
        header=text("DUNGEON  /  IDLE ADVENTURE",12f,gold,true)
        header.letterSpacing=0.12f
        root.addView(header)
        headline=text("",19f,ivory,true).apply{setPadding(dp(3),dp(2),dp(3),dp(8))}
        root.addView(headline)
        scene=ImageView(this).apply{
            scaleType=ImageView.ScaleType.FIT_CENTER
            background=background(0xFF101720.toInt(),0xFF746952.toInt(),13)
            setOnClickListener{showFullscreen()}
        }
        root.addView(scene,LinearLayout.LayoutParams(-1,dp(255)))
        vitality=text("",16f,ivory,true).apply{gravity=Gravity.CENTER;setPadding(dp(5),dp(10),dp(5),dp(4))}
        root.addView(vitality)
        narration=text("",13f,muted).apply{gravity=Gravity.CENTER;maxLines=2;minLines=2}
        root.addView(narration)
        gap(root,7)
        tabs=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        root.addView(tabs,LinearLayout.LayoutParams(-1,dp(51)))
        details=ScrollView(this).apply{isFillViewport=false;overScrollMode=View.OVER_SCROLL_IF_CONTENT_SCROLLS}
        body=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,dp(12),0,dp(12))}
        details.addView(body)
        root.addView(details,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
        updateNumbers();buildTabs();refreshPage()
        UpdateChecker.check(this)
    }
    override fun onResume(){
        super.onResume();running=true;state=DungeonGame.advance(this)
        lastUpdate=0L;handler.removeCallbacks(animator);handler.post(animator)
    }
    override fun onPause(){
        running=false;handler.removeCallbacks(animator);super.onPause()
    }
    override fun onDestroy(){
        fullscreenDialog?.dismiss()
        super.onDestroy()
    }
    private fun showFullscreen(){
        if(fullscreenDialog?.isShowing==true)return
        val dialog=Dialog(this,android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val container=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(base)
            setPadding(dp(12),dp(24),dp(12),dp(22))
        }
        val hint=text("⚔ ЖИВОЕ ПОДЗЕМЕЛЬЕ",20f,gold,true).apply{gravity=Gravity.CENTER}
        container.addView(hint)
        val big=ImageView(this).apply{
            scaleType=ImageView.ScaleType.FIT_CENTER
            background=background(0xFF0E1420.toInt(),0xFF735C42.toInt(),12)
        }
        container.addView(big,LinearLayout.LayoutParams(-1,0,1f))
        val label=text("",16f,ivory,true).apply{
            gravity=Gravity.CENTER;setPadding(dp(10),dp(20),dp(10),dp(16))
        }
        container.addView(label)
        action(container,"✕ ЗАКРЫТЬ БОЛЬШУЮ СЦЕНУ"){dialog.dismiss()}
        dialog.setContentView(container)
        dialog.setOnDismissListener{
            fullscreenDialog=null;fullscreenView=null;fullscreenLabel=null
        }
        fullscreenDialog=dialog;fullscreenView=big;fullscreenLabel=label
        dialog.show()
        dialog.window?.setLayout(-1,-1)
    }
    private fun updateNumbers(){
        window.statusBarColor=base;window.navigationBarColor=base;root.setBackgroundColor(base)
        headline.text="ЭТАЖ "+state.floor+"   ·   "+state.biome
        val hearts=if(state.hp*100/maxOf(1,state.maxHp)<30)"🩸" else "♥"
        vitality.text=hearts+" "+state.hp+"/"+state.maxHp+"    ✦ "+state.level+"    ◈ "+state.gems+"    ◉ "+state.gold
        narration.text=state.log+"\nКОМНАТА "+state.room+"/8  ·  "+state.className+
            (if(state.paused)"  ·  ПАУЗА" else "")+
            (if(state.effectTicks>0)"  ·  "+state.status else "")
    }
    private fun buildTabs(){
        tabs.removeAllViews()
        val labels=listOf("⚔\nПОХОД","▤\nЛУТ","⌂\nЛАГЕРЬ","✦\nГЕРОЙ","⚙\nОПЦИИ")
        labels.forEachIndexed{idx,label->
            val active=page==idx
            val view=text(label,11f,if(active)gold else muted,active).apply{
                gravity=Gravity.CENTER
                background=background(if(active)0xFF354052.toInt() else panel,
                    if(active)0xFFAE8657.toInt() else 0xFF384353.toInt(),10)
                setOnClickListener{switchPage(idx)}
            }
            tabs.addView(view,LinearLayout.LayoutParams(0,-1,1f).apply{marginEnd=dp(3)})
        }
    }
    private fun switchPage(idx:Int){
        if(page!=idx){page=idx;buildTabs();details.scrollTo(0,0)}
        refreshPage()
    }
    private fun change(fn:()->Boolean){
        if(!fn())Toast.makeText(this,"Недостаточно ресурсов или действие недоступно",Toast.LENGTH_SHORT).show()
        state=DungeonGame.advance(this)
        updateNumbers();refreshPage();DungeonWidgetProvider.refreshAll(this)
    }
    private fun refreshPage(){
        if(!::body.isInitialized)return
        lastPageRefresh=System.currentTimeMillis()
        body.removeAllViews()
        when(page){
            0->expedition()
            1->inventory()
            2->camp()
            3->hero()
            else->settings()
        }
    }
    private fun expedition(){
        card(body,"⚔ АВТОМАТИЧЕСКАЯ ЭКСПЕДИЦИЯ",
            "Герой сам выбирает путь, открывает сундуки и сражается. Коснись сцены выше, чтобы открыть увеличенный режим. При гибели экипировка сохраняется."){
            action(this,if(state.paused)"▶ ПРОДОЛЖИТЬ ИССЛЕДОВАНИЕ" else "Ⅱ ПАУЗА"){change{DungeonGame.toggle(this@MainActivity)}}
            action(this,"✚ Использовать зелье · осталось "+state.potions,state.potions>0&&state.hp<state.maxHp){
                change{DungeonGame.usePotion(this@MainActivity)}
            }
        }
        card(body,"🗺 ПУТЬ ПО ПОДЗЕМЕЛЬЮ",
            "Пройдено комнат: "+state.roomsCleared+
                "   ·   Лучшая глубина: "+state.bestFloor+
                "\nУбито врагов: "+state.kills+"   ·   Боссов: "+state.bosses+
                "\nСундуков: "+state.chests+"   ·   Реликвий: "+state.relics+
                "\nКритические удары: "+state.crit+"%   ·   Уклонение: "+state.dodge+"%")
        card(body,"◈ ЕЖЕДНЕВНАЯ ЦЕЛЬ",
            "Победить 10 врагов · "+state.questCount.coerceAtMost(10)+"/10\nНаграда: 100 золота и 2 кристалла"){
            action(this,if(state.questClaimed)"✓ НАГРАДА ПОЛУЧЕНА" else "ЗАБРАТЬ НАГРАДУ",
                state.questCount>=10&&!state.questClaimed){
                change{DungeonGame.claimQuest(this@MainActivity)}
            }
        }
        if(state.offlineReport.isNotBlank())card(body,"☾ ПОКА ТЕБЯ НЕ БЫЛО",state.offlineReport)
        card(body,"✧ БОЕВОЕ СНАРЯЖЕНИЕ",
            "Атака "+state.attack+"   ·   Защита "+state.defense+
                "   ·   Уровень "+state.level+"   ·   Опыт "+state.xp+"/"+(state.level*22)+
                "\nОружие: "+(state.weaponItem?.name?:state.weaponName)+
                "\nБроня: "+(state.armorItem?.name?:state.armorName))
    }
    private fun inventory(){
        card(body,"▤ РЮКЗАК · "+state.gear.size+"/42",
            "Добыча подбирается автоматически. Нажми «Надеть» или «Разобрать». Два предмета одного комплекта дают бонус +4 к атаке и защите.")
        if(state.gear.isEmpty()){
            card(body,"ПОКА ПУСТО","Продолжай исследовать подземелье: предметы выпадают из врагов и сундуков.")
        }
        val sorts=row(body)
        mini(sorts,if(sortMode==0)"✓ РЕДКОСТЬ" else "РЕДКОСТЬ"){sortMode=0;refreshPage()}
        mini(sorts,if(sortMode==1)"✓ СИЛА" else "СИЛА"){sortMode=1;refreshPage()}
        mini(sorts,if(sortMode==2)"✓ ТИП" else "ТИП"){sortMode=2;refreshPage()}
        gap(body,9)
        val gearSorted=when(sortMode){
            1->state.gear.sortedByDescending{it.power}
            2->state.gear.sortedWith(compareBy<Gear>{it.slot}.thenByDescending{it.rarity})
            else->state.gear.sortedWith(compareByDescending<Gear>{it.rarity}.thenByDescending{it.power})
        }
        gearSorted.forEach{item->
            val equipped=state.equipment[item.slot]==item.id
            val star=when(item.rarity){4->"★★★★★";3->"★★★★";2->"★★★";1->"★★";else->"★"}
            val slot=when(item.slot){"weapon"->"⚔ Оружие";"armor"->"⛨ Броня";"ring"->"◈ Кольцо";else->"✧ Амулет"}
            card(body,star+"  "+item.name,
                slot+" · "+DungeonGame.rarityName(item.rarity)+
                    " · сила "+item.power+" · "+item.trait+
                    if(item.set>0)" · комплект "+item.set else ""){
                if(equipped)addLine(this,"✓ НАДЕТО",12f,0xFF9CD6AB.toInt(),true)
                else{
                    val buttons=row(this)
                    mini(buttons,"НАДЕТЬ"){change{DungeonGame.equip(this@MainActivity,item.id)}}
                    mini(buttons,"РАЗОБРАТЬ"){change{DungeonGame.salvage(this@MainActivity,item.id)}}
                }
            }
        }
    }
    private fun camp(){
        card(body,"⌂ ЛАГЕРЬ ГЕРОЯ",
            "Постоянные улучшения сохраняются после смерти. Золото: "+state.gold+" · Материалы: "+state.shards)
        val improvements=listOf(
            Triple("camp","⛺ Укрепить лагерь · +6 HP",state.camp),
            Triple("forge","⚒ Улучшить кузницу · +2 атаки",state.forge),
            Triple("healer","✚ Лечебница · +10 HP",state.healer),
            Triple("scouts","◉ Разведчики · лучше добыча",state.scouts)
        )
        for((key,title,level)in improvements){
            card(body,title,"Уровень "+level+" · Цена "+(80+level*85)+" золота"){
                action(this,"УЛУЧШИТЬ",state.gold>=80+level*85&&level<25){
                    change{DungeonGame.upgrade(this@MainActivity,key)}
                }
            }
        }
        card(body,"⚒ КУЗНИЦА",
            "Материалы: "+state.shards+" · Цена: "+(5+state.forge*2)+" материалов и "+(30+state.forge*12)+" золота"){
            action(this,"ВЫКОВАТЬ СЛУЧАЙНОЕ СНАРЯЖЕНИЕ",
                state.shards>=5+state.forge*2&&state.gold>=30+state.forge*12){
                change{DungeonGame.forgeItem(this@MainActivity)}
            }
            action(this,"УСИЛИТЬ НАДЕТОЕ ОРУЖИЕ · "+(20+state.level*3)+" ЗОЛОТА · "+(4+state.forge)+" МАТЕРИАЛОВ",
                state.weaponItem!=null&&state.gold>=20+state.level*3&&state.shards>=4+state.forge){
                change{DungeonGame.improveWeapon(this@MainActivity)}
            }
        }
        card(body,"✥ СТРАНСТВУЮЩИЙ ТОРГОВЕЦ",
            "Предложения обновляются каждый день. Доступно три товара и лечебные зелья."){
            action(this,"КУПИТЬ ЗЕЛЬЕ · "+(25+state.floor*2)+" МОНЕТ",
                state.gold>=25+state.floor*2){change{DungeonGame.buyPotion(this@MainActivity)}}
            for(idx in 0..2){
                val item=DungeonGame.merchantOffer(state,idx)
                val price=70+state.floor*3+idx*25
                action(this,item.name+" · +"+item.power+" · "+price+" ЗОЛОТА",
                    state.gold>=price){change{DungeonGame.buyOffer(this@MainActivity,idx)}}
            }
        }
    }
    private fun hero(){
        card(body,"✦ ПЕРСОНАЖ · "+state.className,
            "Уровень "+state.level+"  ·  Опыт "+state.xp+"/"+(state.level*22)+
                "\nСила "+state.attack+"  ·  Защита "+state.defense+"  ·  Здоровье "+state.maxHp)
        card(body,"ВЫБЕРИ КЛАСС",
            "Воин: здоровье и броня. Следопыт: крит и уклонение. Маг: повышенная атака."){
            val group=row(this)
            listOf("ВОИН","СЛЕДОПЫТ","МАГ").forEachIndexed{idx,title->
                mini(group,(if(idx==state.heroClass)"✓ " else "")+title){
                    change{DungeonGame.chooseClass(this@MainActivity,idx)}
                }
            }
        }
        card(body,"ДЕРЕВО НАВЫКОВ",
            "Очки навыков: "+DungeonGame.skillPoints(state).coerceAtLeast(0)+
                " · Получай одно каждые два уровня"){
            listOf(
                "⚔ Мастерство боя · ур. "+state.skillAttack+" · атака +3",
                "⛨ Стойкость · ур. "+state.skillDefense+" · защита +2",
                "◈ Удача · ур. "+state.skillLuck+" · крит и уворот"
            ).forEachIndexed{idx,title->
                action(this,title,DungeonGame.skillPoints(state)>0){
                    change{DungeonGame.train(this@MainActivity,idx)}
                }
            }
        }
        card(body,"🏆 ДОСТИЖЕНИЯ · "+state.achievements.size+"/8",
            if(state.achievements.isEmpty())"Первое достижение появится после первой победы."
                else state.achievements.sorted().joinToString("   •   "))
        card(body,"☠ БЕСТИАРИЙ · "+state.discovered.size+"/20",
            if(state.discovered.isEmpty())"Здесь появятся встреченные монстры."
                else state.discovered.sorted().joinToString("   •   "))
        card(body,"ИСТОРИЯ",
            "Комнат: "+state.roomsCleared+" · Падений: "+state.deaths+
                " · Боссов: "+state.bosses+" · Реликвий: "+state.relics+
                "\nРекордная глубина: "+state.bestFloor+" · Кристаллов: "+state.gems)
    }
    private fun settings(){
        card(body,"⚙ АВТОМАТИЧЕСКАЯ ИГРА",
            "Виджет не расходует батарею непрерывной анимацией. Игра учитывает время между запусками."){
            addLine(this,"Автоматически пить зелье при HP "+state.autoPotion+"%",13f,muted)
            val thresholdRow=row(this)
            listOf(25,45,65).forEach{n->mini(thresholdRow,""+n+"%"){
                change{DungeonGame.changeSettings(this@MainActivity,potion=n)}
            }}
            action(this,"Авторазбор обычных предметов: "+if(state.autoSalvage)"ВКЛ" else "ВЫКЛ"){
                change{DungeonGame.changeSettings(this@MainActivity,auto=!state.autoSalvage)}
            }
            action(this,"Режим экономии батареи: "+if(state.eco)"ВКЛ" else "ВЫКЛ"){
                change{DungeonGame.changeSettings(this@MainActivity,eco=!state.eco)}
            }
        }
        card(body,"🎨 ОФОРМЛЕНИЕ"){
            addLine(this,"Тема приложения:",13f,muted)
            val themeButtons=row(this)
            listOf("СУМРАК","НОЧЬ","ЛЕС").forEachIndexed{idx,t->
                mini(themeButtons,t){change{DungeonGame.changeSettings(this@MainActivity,theme=idx)}}
            }
            gap(this,8)
            addLine(this,"Непрозрачность фона виджета: "+state.opacity+"%",13f,muted)
            val alphaButtons=row(this)
            listOf(40,70,100).forEach{pct->mini(alphaButtons,""+pct+"%"){
                change{DungeonGame.changeSettings(this@MainActivity,opacity=pct)}
            }}
        }
        card(body,"▣ СОХРАНЕНИЯ","Создай резервную копию в памяти телефона или восстанови ранее сохранённый файл."){
            action(this,"↥ ЭКСПОРТ СОХРАНЕНИЯ"){
                val i=Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
                    addCategory(Intent.CATEGORY_OPENABLE);type="application/json"
                    putExtra(Intent.EXTRA_TITLE,"DungeonWidget-save.json")
                }
                startActivityForResult(i,101)
            }
            action(this,"↧ ВОССТАНОВИТЬ ИЗ ФАЙЛА"){
                val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
                    addCategory(Intent.CATEGORY_OPENABLE);type="*/*"
                }
                startActivityForResult(i,102)
            }
        }
        card(body,"⬇ ОБНОВЛЕНИЯ","Версия приложения и новые APK публикуются на GitHub."){
            action(this,"ПРОВЕРИТЬ НОВУЮ ВЕРСИЮ"){UpdateChecker.check(this@MainActivity,true)}
        }
    }
    @Deprecated("Legacy file picker result")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK)return
        val uri=data?.data?:return
        try{
            when(requestCode){
                101->{
                    contentResolver.openOutputStream(uri)?.use{
                        it.write(DungeonGame.exportSave(this).toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(this,"Сохранение экспортировано",Toast.LENGTH_SHORT).show()
                }
                102->{
                    val raw=contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}?:return
                    AlertDialog.Builder(this).setTitle("Восстановить прогресс?")
                        .setMessage("Текущий прогресс будет заменён выбранным сохранением.")
                        .setPositiveButton("ВОССТАНОВИТЬ"){_,_->
                            if(DungeonGame.importSave(this,raw)){
                                state=DungeonGame.advance(this);updateNumbers()
                                refreshPage();DungeonWidgetProvider.refreshAll(this)
                            }else Toast.makeText(this,"Неверный файл сохранения",Toast.LENGTH_LONG).show()
                        }.setNegativeButton("Отмена",null).show()
                }
            }
        }catch(_:Exception){Toast.makeText(this,"Ошибка чтения или записи файла",Toast.LENGTH_LONG).show()}
    }
}
