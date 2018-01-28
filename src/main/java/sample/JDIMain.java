package sample;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;
import sample.target.A123;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class JDIMain{
    public static void main(String... args)throws Exception{
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> arguments = connector.defaultArguments();
        //ターゲットVMのクラスパス。環境に応じて調整が必要かも。
        arguments.get("options").setValue("-classpath build/classes/java/main");
        arguments.get("main").setValue(A123.class.getCanonicalName());
        VirtualMachine vm = connector.launch(arguments);
        EventRequestManager reqman = vm.eventRequestManager();


        //クラスロード時にイベントを発生させる
        ClassPrepareRequest classreq = reqman.createClassPrepareRequest();
        classreq.addClassFilter(A123.class.getCanonicalName());
        classreq.enable();

        EventQueue queue = vm.eventQueue();

        //ターゲットVMの標準出力を読み取って、このVMの標準出力に出力する
        InputStream in = vm.process().getInputStream();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try(Reader reader = new InputStreamReader(in)){
                int c;
                while((c = reader.read()) != -1)System.out.print((char)c);
            }catch(Exception e){}
        });
        try{
            while(true){
                EventSet events = queue.remove();
                for(Event ev : events){
                    if(ev instanceof ClassPrepareEvent){
                        //ifの判定箇所にBreakPointを設定する
                        ReferenceType type = ((ClassPrepareEvent) ev).referenceType();
                        Method mt = type.methodsByName("main").get(0);
                        create(reqman, mt, 2);
                        create(reqman, mt, 7);
                        create(reqman, mt, 12);

                    }else if(ev instanceof BreakpointEvent){
                        //aの値を取得して、1増やした値を再設定する
                        StackFrame fr = ((BreakpointEvent) ev).thread().frame(0);
                        LocalVariable a = fr.visibleVariableByName("a");
                        int iv = ((IntegerValue)fr.getValue(a)).value();
                        fr.setValue(a, vm.mirrorOf(iv + 1));
                    }
                }
                events.resume();
            }

        }catch(VMDisconnectedException vmdis){
            //VM Exit
        }finally{
            executor.shutdown();
        }
    }

    private static BreakpointRequest create(EventRequestManager reqman, Method m, long index){
        BreakpointRequest bpreq = reqman.createBreakpointRequest(m.locationOfCodeIndex(index));
        bpreq.enable();
        return bpreq;
    }
}
