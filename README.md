# JDIでa == 1 &amp;&amp; a == 2 &amp;&amp; a == 3をtrueにする

JDI(Java Debug Interface)はIDEのデバッグ実行モード等で使用されるAPIです。
JDIでは実行中の変数の書き換えも可能なので、a == 1 &amp;&amp; a == 2 &amp;&amp; a == 3 をtrueにできます。

JDIMain.java を実行すると別プロセスでA123.javaが(いわゆるデバッグモードで)実行されます。
このプログラムでは、if文の途中にブレークポイントを仕込み、ブレークポイントのたびにaの値を増やすことで前述の条件がすべて成り立つようにしています。
