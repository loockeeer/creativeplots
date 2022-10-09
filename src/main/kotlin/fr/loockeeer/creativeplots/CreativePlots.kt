package fr.loockeeer.creativeplots

import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler

class CreativePlots : JavaPlugin(), CommandExecutor, Listener {
    private var visitors: Set<String> = mutableSetOf()

    companion object {
        val guiName = "${ChatColor.GRAY}${ChatColor.BOLD}>>${ChatColor.RESET}${ChatColor.GOLD} Menu des mondes"
        val paddingMaterial = Material.LIGHT_GRAY_STAINED_GLASS_PANE
    }

    fun getWorld(player: Player): World? {
        return server.getWorld("plot-${player.name.lowercase()}")
    }

    fun fetchWorld(player: Player): World? {
        return fetchWorld("plot-${player.name.lowercase()}}")
    }

    fun fetchWorld(name: String): World? {
        return WorldCreator(name).run {
            generateStructures(false)
            hardcore(false)
            type(WorldType.FLAT)
            environment(World.Environment.NORMAL)

            return@run createWorld()?.also {
                it.difficulty = Difficulty.PEACEFUL
                it.save()
                it.worldBorder.size = 500.0
                it.time = 1000
                it.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
                it.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
                it.setGameRule(GameRule.DO_FIRE_TICK, false)
                it.setGameRule(GameRule.DO_MOB_SPAWNING, false)
                it.setGameRule(GameRule.DO_TRADER_SPAWNING, false)
                it.setGameRule(GameRule.DO_PATROL_SPAWNING, false)
                it.setGameRule(GameRule.DO_INSOMNIA, false)
                it.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false)
                it.setGameRule(GameRule.MOB_GRIEFING, false)
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        getCommand("world")?.setExecutor(this)
        Bukkit.getPluginManager().registerEvents(this, this)
        server.getWorld("world")!!.worldBorder.size = 10.0
        this.server.worldContainer.listFiles()?.forEach { file ->
            if(file.isDirectory && file.name.startsWith("plot-")) {
                fetchWorld(file.name)
                println("Loaded world plot ${file.name}")
            }
        }

        println("--- World List ---")
        this.server.worlds.forEach {
            println("- ${it.name}")
        }
        println("------------------")

        server.getWorld("world")?.let {
            it.worldBorder.size = 10.0
            it.spawnLocation = Location(it, 0.0, 100.0, 0.0)
            it.time = 1000
            it.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
            it.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
            it.setGameRule(GameRule.DO_FIRE_TICK, false)
            it.setGameRule(GameRule.DO_MOB_SPAWNING, false)
            it.setGameRule(GameRule.DO_TRADER_SPAWNING, false)
            it.setGameRule(GameRule.DO_PATROL_SPAWNING, false)
            it.setGameRule(GameRule.DO_INSOMNIA, false)
            it.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false)
            it.setGameRule(GameRule.MOB_GRIEFING, false)
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player) {
            sender.sendMessage("${ChatColor.GRAY}${ChatColor.BOLD}>> ${ChatColor.RESET}${ChatColor.GOLD}Vous devez être un joueur pour faire cela !")
            return true
        }

        val inventory = server.createInventory(null, InventoryType.HOPPER, guiName)
        for (i in (0 until inventory.size)) {
            inventory.setItem(i, ItemStack(paddingMaterial).also {
                it.itemMeta = it.itemMeta?.also {
                    it.setDisplayName(" ")
                }
            })
        }
        inventory.setItem(0, ItemStack(Material.BEACON).also {
            it.itemMeta = it.itemMeta?.also {
                it.setDisplayName("Retourner au hub")
            }
        })
        if(server.getWorld("plot-${sender.name}") == null) {
            inventory.setItem(2, ItemStack(Material.GRASS_BLOCK).also {
                it.itemMeta = it.itemMeta?.also {
                    it.setDisplayName("Créer un monde")
                }})
        } else {
            inventory.setItem(2, ItemStack(Material.OAK_DOOR).also {
                it.itemMeta = it.itemMeta?.also {
                    it.setDisplayName("Se téléporter")
                }
            })
            inventory.setItem(4, ItemStack(Material.DIAMOND).also {
                it.itemMeta = it.itemMeta?.also {
                    it.setDisplayName("Visiter un monde")
                }
            })
        }
        sender.openInventory(inventory)
        return true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlayerChat(e: AsyncPlayerChatEvent) {
        if(!visitors.contains(e.player.name)) return
        if (e.message != "q") {
            val world = getWorld(e.player)
            if (world == null) {
                e.player.sendMessage("${ChatColor.GRAY}${ChatColor.BOLD}>>${ChatColor.RESET}${ChatColor.GOLD} Ce joueur n'a pas de monde !")
                return
            } else {
                e.player.sendMessage("${ChatColor.GRAY}${ChatColor.BOLD}>>${ChatColor.RESET}${ChatColor.GOLD} Téléportation en cours")
                Bukkit.getScheduler().runTask(this, Runnable { e.player.teleport(world.spawnLocation) })
            }
        } else {
            e.player.sendMessage("${ChatColor.GRAY}${ChatColor.BOLD}>>${ChatColor.RESET}${ChatColor.GOLD} Téléportation annulée")
        }
        e.isCancelled = true
        visitors = visitors.minusElement(e.player.name)
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onInventoryClick(e: InventoryClickEvent) {
        println(e.view.title)
        println(guiName)
        println(e.view.title == guiName)
        val inventory = e.inventory
        if(e.view.title.contains("Menu des mondes") && inventory == e.view.topInventory) {
            val player = e.whoClicked as Player
            val clicked = e.currentItem
            println("Menu clicked")
            when(clicked?.type) {
                Material.BEACON -> {
                    Bukkit.getScheduler().runTask(this, Runnable {
                        val world = server.getWorld("world")!!
                        player.closeInventory()
                        player.location.world?.save()
                        player.sendMessage("${ChatColor.GRAY}${ChatColor.BOLD}>>${ChatColor.RESET}${ChatColor.GOLD} Téléportation en cours !")
                        player.teleport(world.spawnLocation)
                    })
                }
                Material.GRASS_BLOCK -> {
                    println("Grass clicked")
                    Bukkit.getScheduler().runTask(this, Runnable {
                        if(getWorld(player) != null) return@Runnable
                        val world = WorldCreator("plot-${e.whoClicked.name}").run {
                            generateStructures(false)
                            hardcore(false)
                            type(WorldType.FLAT)
                            environment(World.Environment.NORMAL)
                        }.createWorld()!!
                        world.save()
                        world.difficulty = Difficulty.PEACEFUL
                        world.worldBorder.size = 500.0
                        e.isCancelled = true

                        player.closeInventory()
                        player.sendMessage("${ChatColor.GRAY}${ChatColor.BOLD}>>${ChatColor.RESET}${ChatColor.GOLD} Téléportation en cours !")
                        player.teleport(world.spawnLocation)
                    })
                }
                Material.OAK_DOOR -> {
                    Bukkit.getScheduler().runTask(this, Runnable {
                        if(getWorld(player) == null) return@Runnable
                        player.closeInventory()
                        player.location.world?.save()
                        player.sendMessage("${ChatColor.GRAY}${ChatColor.BOLD}>>${ChatColor.RESET}${ChatColor.GOLD} Téléportation en cours !")
                        player.teleport(server.getWorld("plot-${player.name}")!!.spawnLocation)
                    })
                }
                Material.DIAMOND -> {
                    Bukkit.getScheduler().runTask(this, Runnable {
                        player.closeInventory()
                        visitors += player.name
                        player.sendMessage("${ChatColor.GRAY}${ChatColor.BOLD}>>${ChatColor.RESET}${ChatColor.GOLD} Entrez le pseudo du joueur dont vous souhaitez visiter le monde ou \"q\" pour annuler")
                    })
                }
            }
            e.isCancelled = true
        }
    }
}