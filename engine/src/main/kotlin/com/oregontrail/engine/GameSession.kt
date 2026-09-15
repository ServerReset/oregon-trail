package com.oregontrail.engine

/**
 * Transient front-end session state: settings and meta progression, the save
 * slots and pending requests the UI acts on, navigation return points, the
 * animation clock and the short-lived screens (notices, choices, minigames).
 *
 * Kept apart from the journey itself in [GameState] so each reads on its own.
 */
open class GameSession {

    // ----- settings and meta progression --------------------------------
    var soundEnabled: Boolean = true
    var pendingSound: Sound? = null
    val achievements: MutableSet<String> = LinkedHashSet()
    var stats: GameStats = GameStats()

    // ----- save slots and pending front-end requests --------------------
    /** Save states supplied by the front-end for the load screen. */
    var saveSlots: List<SaveSlot> = emptyList()

    /** True when the front-end has an autosave to continue. */
    var autosaveAvailable: Boolean = false

    /** Current page of the saved-games list. */
    var loadPage: Int = 0

    /** Set when the player asks to rename a slot; the front-end shows a dialog. */
    var requestedRenameId: String? = null
        internal set

    /** Set when the player asks to overwrite a slot with the current game. */
    var requestedOverwriteId: String? = null
        internal set

    /** Seed for the Trail of the Day, supplied by the front-end. */
    var dailySeed: Long = 0L

    var requestedAutosaveLoad: Boolean = false
        internal set
    var requestedSave: Boolean = false
        internal set
    var requestedLoadId: String? = null
        internal set
    var requestedDeleteId: String? = null
        internal set

    /** Name of an achievement just unlocked, for the front-end to toast. */
    var pendingUnlock: String? = null
    var requestedShare: Boolean = false
        internal set
    var requestedQuickSave: Boolean = false
        internal set
    var requestedQuickLoad: Boolean = false
        internal set

    /** Presentation settings supplied by the front-end (may be null in tests). */
    var uiSettings: UiSettings? = null
    var requestedNameEdit: Int? = null
        internal set

    /** True when the front-end should prompt for a gravestone epitaph. */
    var requestedEpitaphEdit: Boolean = false
        internal set

    // ----- navigation returns and animation clock -----------------------
    internal var pauseReturn: Phase = Phase.TRAVEL
    internal var postcardReturn: Phase = Phase.TRAVEL
    internal var managementReturn: Phase = Phase.TITLE
    internal var loadReturn: Phase = Phase.TITLE
    internal var factsRead = 0

    /** Animation clock, advanced by the front-end on a timer. */
    var frame: Int = 0
        internal set

    /** When true, the front-end gets a top-to-bottom reveal on screen changes. */
    var transitions: Boolean = false

    /** Optional illustration shown on the current notice screen. */
    var noticeArt: List<String>? = null
        internal set

    internal var lastRenderedPhase: Phase? = null
    internal var transitionStart = 0
    internal var lastEventArt: List<String>? = null
    internal var arrivalFrame = -1

    // ----- transient UI state -------------------------------------------
    internal var aboutPage = 0
    internal var journalPage = 0
    internal var noticeTitle = ""
    internal val noticeLines = ArrayList<String>()
    internal var noticeNext: Phase = Phase.TRAVEL
    internal var choiceTitle = ""
    internal val choiceLines = ArrayList<String>()
    internal val choiceOptions = ArrayList<Pair<String, String>>()
    internal var choiceNext: Phase = Phase.TRAVEL

    internal var huntField: HuntField? = null
    internal var huntReturn = Phase.TRAVEL
    internal var huntDays = 1

    internal var raftField: RaftField? = null
    internal var barlowField: BarlowField? = null
    internal var cutoffTarget: Int = -1

    internal var deathCause = ""
}
