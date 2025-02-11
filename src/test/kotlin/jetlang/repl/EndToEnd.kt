package jetlang.repl

import androidx.compose.ui.test.*
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TestEndToEnd {
    @Test
    fun reduce() = runReplTest {

        val command = "reduce(range, initial, previous next -> (previous + previous)*(next + next))"
        evaluate(
            """
            var start = 2
            var end = 10
            var initial = 3
            var range = {start * 2, end}
            print "range ="
            out range
            print "initial ="
            out initial
            
            print "computing: $command"
            var result = $command
            print "and the result is....."
            out result 
            """.trimIndent()
        )
        outputSection.onChildren().apply {
            get(0).assertTextEquals("range =")
            get(1).assertTextEquals("{4, 10}")
            get(2).assertTextEquals("initial =")
            get(3).assertTextEquals("3")
            get(4).assertTextEquals("computing: $command")
            get(5).assertTextEquals("and the result is.....")
            get(6).assertTextEquals("29727129600")
            assertCountEquals(7)
        }
    }

    @Test
    fun pi() = runReplTest {
        evaluate("""
            var n = 5000
            var sequence = map({0, n}, i -> -1^i / (2 * i + 1))
            var pi = 4 * reduce(sequence, 0, x y -> x + y)
            print "pi = "
            out pi
            print "done"
            """.trimIndent())
        waitUntilAtLeastOneExists(hasText("done"))
        waitForIdle()
        outputSection.onChildren().apply {
            get(0).assertTextEquals("pi = ")
            get(1).assertTextEquals("3.1417926135957928384026393958794601181977798588305536196565227621459315966338194579669985248106691691102438278394362191534540367263098243375648299685749809579991183162249286760950548356315626803524701017684162609900767061356695130488110490180313058487008819520131711095436817811558819250944014105757382715124916931192307651058930546452257235592878336613145692491618678404531074658086675708064192395354978462520279864468567011527096672135646885589980892160526390118853731184469439684891350681487247562165182636585526933408961068605278811361658302323998542644918848884007795249174183461142218144435282983895380387171240500971708723716595371267523647357838503131138856386129505247905787696884638681444919078014262123185984560299317891175468227909494978574135029844258110313153668272578629343727634992065541044601693104099836383724528287579302504408282844557655961548231613121259744281437631207407684480519849811655592168440506984296098148276329627548791210430937770176546498534764264821304145951425290360706385372129922895779983131530074400168939278534451215256515101922670549779821119696458247614363391125559997708796208274120563110697405002823546763391729093772546481261760318510234074276952324958233900782766990609730151036751565058754419856895616452985556226733074465158698791309854737280952472388982633524764126245341748342569287654182106719252958739412753448508344415199361770042179350722820982562367848576225367303406770314283955670003184509521988493619390688464247387664878956036690607634568220456283292484090376691950296084228986558530028744347006228541275152780056348627445789986385979682648207126790757286479490991204042906836761381002724504379464877500930526377458962431028567922335453613249160082667079588496914349450388437743466716180386183025856464081835517279512481156910740561403936810686186449693398068316483985424872879350398322272200281332443675108591673057251740095331575584531539076095447154937286723278430623139765532548316978766684759595158690300731054932335444991656207046158701842707592432873809115652666342674386897955048484687642819262141759334460059799064374134622206409209169631089347845676720338983695637217061820258224847215515749025787035911943204888340815005521465000689571809229582690950198480510117498587389743284946684464391597876309522607839487389327799182791026824909620457652328092655545934782906303561886051658337205792338269708476558334573616207890497859829595615140707887805288755654874353175952828697591004915809175481319240829240402509579211933810644484682239291170937814318211754048676907476240568495129027841752969521632761554622935386718998937761821760289699260339425212034972152627610421846366911059886507894885729413211434221524944084170285591660514506914054246665302371395587501694246931147369474132863581546937299961074459786426482212796088909129089416967605831120425533480864389366248426165813140564634065025728878989646673014349910635557169829908401170739967735615977404724188847375401209245560134101007903335861912176425748188135189549365226934428430904419686397454756622973386222512558869765600832516231865152520041461693415409598780638793660944980742863677038236966267540911514554523870277165285805638565687930952261048249896724968886073923281015655304565791928864368359766188329555243328194866650494782338280679219720949270749097627816259925251016488458541151511093385216470729688681118478645516567513738136235725989759160513193981271726456233121899675742719569905531692730587026943677127070599254469347685289029492268918896332454457437594158490913888022673840628015485753187205493338074323191559418849213422947103552512140551796372560585529339376439783938665235750256405644519797930869660457920525252619506356713270188473449966344644473071548069026508272715561077404608085180743940518041842136264420315188736107852547927607256274662074791037759377654589327178092017768591448804499238371020210406925399353033771696371716499716305840955455284644940811105864896992255049509690798900995662221443689685891133085813968852196606934252151255737830063769067380922214660856368189103714320323743472204327381827499007068754821829496461561842954654073153960565090207431017163004544520151890343583218635155080975735295841750779624845203937191232304454959409725891796152263256039847583835148414526357718236975627559281652080222883199755582255299965238703342180824786572345989629695339932014960272011793111")
            get(2).assertTextEquals("done")
            assertCountEquals(3)
        }
    }
}
