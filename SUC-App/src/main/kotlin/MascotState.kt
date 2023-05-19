import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking

sealed interface MascotEventType {
    //なにもないよ
    object None : MascotEventType
    object Run : MascotEventType

    //ランダムに喋る
    object Chat : MascotEventType
    data class Speak(val text: String) : MascotEventType
    object Gaming : MascotEventType

    //転ぶ
    object Fall : MascotEventType

    //爆発
    object Explosion : MascotEventType

    object DVD : MascotEventType

}


class MascotState(mascotEventType: MascotEventType) {
    private val stateFlow = MutableStateFlow(mascotEventType)
    val flow get() = stateFlow.asStateFlow()
    private val server = Server()
    suspend fun initServer() {
        server.onEventReceive {
            runBlocking {
                when (val e = it.event) {
                    is Event.FailedBuild -> TODO()
                    is Event.OpenProject -> {
                        speak(e.projectName + "を開きました！", 5000, true)
                        //change(Speak(e.projectName))
                    }

                    is Event.StartBuild -> TODO()
                    is Event.SuccessBuild -> TODO()
                    is Event.Typed -> {
                        feed(e.char)
                    }
                }
            }

        }
        server.runServer()

    }

    //nullで吹き出し非表示
    private val serif = MutableStateFlow<String?>(null)
    val serifFlow = serif.asStateFlow()
    suspend fun speak(string: String, delayMillis: Long, important: Boolean = false) {
        if (important) {
            serif.emit(string)
        } else if (serif.value == null) {
            serif.emit(string)
        } else return
        delay(delayMillis)
        serif.emit(null)
    }


    private var previousEventType by mutableStateOf<MascotEventType>(MascotEventType.None)
    suspend fun change(state: MascotEventType) {
        previousEventType = stateFlow.value
        stateFlow.emit(state)
    }

    suspend fun recoverEvent() {
        stateFlow.emit(previousEventType)
    }


    val charFlow= MutableSharedFlow<Char>()
    suspend fun feed(char: Char) {
        charFlow.emit(char)
    }
}

@Composable
fun rememberMascotState(
    initialMascotEventType: MascotEventType = MascotEventType.None
) =
    remember { MascotState(initialMascotEventType) }

val texts = arrayOf(
    "カップルでディズニーに行くとすぐ別れるっていうよね。",
    "もぅﾏﾁﾞ無理...コンパイルしょ...",
    "小梅太夫「チャンチャカチャンチャンチャチャンカチャンチャン床に抜け毛が落ちていると思っていたら～～～wwwwww～～…超弦理論でした～～～(11次元宇宙を近くする小梅太夫)あああああﾁｸｼｮｵｵｵｵｵｵｵｵｵｵ超弦理論→～～～",
    "技術的には可能です(ｷﾘｯ)",
    "ﾄﾞﾋｭｩｩｩｩﾝシンフォギアァァァァ!!!ｷｭｷｭｷｭｷｭｲﾝ!ｷｭｷｭｷｭｷｭｲﾝ!ｷｭｷｭｷｭｷｭｷｭｷｭｷｭｷｭｷｭｷｭｷｭｷｭｷｭｲﾝ!\n" + "ﾎﾟｫﾛﾎﾟﾎﾟﾎﾟﾎﾟﾍﾟﾍﾟﾍﾟﾍﾟﾋﾟﾋﾟﾋﾟﾋﾟﾋﾟｰﾍﾟﾍﾟﾍﾟﾍﾟﾍﾟﾍﾟﾍﾟﾍﾟｰ♪",
    "も　う　ダ　メ　ぽ",
    "♪～",
    "ｾｰﾝｷｮ❗\uFE0Fｾﾝｷｮ❗\uFE0Fｱｶﾙｲｾﾝｷｮｰ‼\uFE0Fｾｰﾝｷｮ❗\uFE0Fｾﾝｷｮ❗\uFE0Fｱｶﾙｲｾﾝｷｮｰ‼\uFE0F⤴\uFE0F",
    "すごい楽しい素晴らしいソフトがあるんですよBlenderっていうんですけどね，",
    "あっ産地が直送されてきた",
    "消しゴムマジックで消してやるのさ☆",
    "俺モテすぎるから誰からもチョコ受け取らないんだよね",
    "お前守るよ(ｲｹｳﾞｫ)",
    "Twitterは脳を粉々に破壊するよ",
    "レターパックで現金送れ"
)