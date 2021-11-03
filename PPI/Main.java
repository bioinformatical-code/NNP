import java.io.File;

public class Main {
    public static void main(String[] args) {
        Path.printLine("hello");

        File file = new File(Path.inputPath);
        AMGraph aMGraph = new AMGraph(file);

        aMGraph.AdjacentNodes();
        aMGraph.CommonNodes();
        aMGraph.UnionNodes();
        aMGraph.T_sim();
        aMGraph.S_sim();
        aMGraph.weight();
        aMGraph.WPC_1();
        aMGraph.WPC_2();
        aMGraph.WPC_3();

//        aMGraph.calculate();
        aMGraph.WPC_CYC();
    }
}