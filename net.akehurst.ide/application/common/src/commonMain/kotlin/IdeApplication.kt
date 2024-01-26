package net.akehurst.ide

import net.akehurst.ide.gui.Gui

class IdeApplication {

    //val ide=  IdeCore()
    val gui = Gui()

    suspend fun  start() {
       gui.start()
    }

}