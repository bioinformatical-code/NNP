import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.util.*;

public class AMGraph {

	private static final float Thres_WR = (float) 0.2;
	private static final float NA_WR = (float) 0.8;//改进，应用0.2
//	private static final float Th_OS = (float) 0.2;// 匹配阈值
	public Map<String, Integer> vexsMap = new HashMap<String, Integer>();// 点集
	public int[][] arcs = null; // 边集
	public Map<String, List<String>> nodeNotes = new HashMap<String, List<String>>();// 节点—注释
	public Map<String, Integer> Note = new HashMap<String, Integer>();// 注释出现次数
	public List<String> Notes = new ArrayList<String>(); // 注释库

	public Map<String, Integer> AdjacetNode = new HashMap<String, Integer>();// 一阶邻居节点(度)
	public int[][] CommonNode = null;// 共同邻居节点
	public int[][] UnionNode = null;// 并集邻居节点
	public float[][] t_sims = null;// 拓扑结构相似性

	public float[][] s_sims = null;// 语义相似性
	public float[][] weights = null;// 权值
	public float Th = 0;// 自适应阈值
	public float Recall = 0;// 回召率
	public float Precision = 0;// 准确率
	public float F_measure = 0;// 综合评价指标
	public Map<String, Float> Matrix = new HashMap<String, Float>();// 节点平均加权度
	public List<String> Seeds = new ArrayList<String>(); // 种子节点队列
	public List<List<String>> PC = new ArrayList<List<String>>(); // 算法识别复合物集合
	public List<List<String>> CYC = new ArrayList<List<String>>(); // 标准复合物集合
//	public List<List<String>> PC1 = new ArrayList<List<String>>(); // 算法识别复合物集合，对比时才解注释掉，正常注释掉
	public AMGraph(File file) {//对比实验时只留CYC
		loadNode(file);
		loadEdge(file);
		loadAnnotation(file);
		loadCYC(file);
//		Path.printLine("Loading PPI Net successful!\n" + "The First step of WPC has finished!\n"+ "----------------------------------------------------------------------------------------------------------------\n");
	}

//	public void calculate(){//对比时解除注释，并在下面CYC中修改PC为PC1
//		File file = new File("/Users/nan/Desktop/MCode/test.txt");
//		try {
//			FileInputStream fileInputStream = new FileInputStream(file);
//			InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
//			BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//
//			StringBuffer sb = new StringBuffer();
//			String text = null;
//			while((text = bufferedReader.readLine()) != null){
//				String a[] = text.split(" ");
//				List<String> A = new ArrayList<String>();
//				for(int i=0;i<a.length;i++){
//					A.add(a[i]);
//				}
//				PC1.add(A);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

	public void loadEdge(File file) {
		try {
			InputStream io = new FileInputStream(file.getAbsoluteFile());
			XSSFWorkbook workbook = new XSSFWorkbook(io);
			// 加载边集
			// 记录两端点的位置
			int vex1Site = 0;
			int vex2Site = 0;
			// 记录输入两端点
			String vex1 = null;
			String vex2 = null;
			Sheet sheet = workbook.getSheetAt(0);
			int firstrow = sheet.getFirstRowNum();
			int lastrow = sheet.getLastRowNum();
			int length = lastrow - firstrow + 1;
			this.arcs = new int[length][length];
			for (int i = 0; i < length; i++) { // 初始化边集
				for (int j = 0; j < length; j++) {
					this.arcs[i][j] = 0; // 0表示该位置所对应的两顶点之间没有边
				}
			}
			// System.out.println("边集：");
			for (int i = firstrow; i < lastrow + 1; i++) {
				Row row = sheet.getRow(i);
				if (row != null) {
					Cell cellA = row.getCell(0);
					Cell cellB = row.getCell(1);
					vex1 = cellA.toString();
					vex2 = cellB.toString();
					// System.out.println(vex1 + "---" + vex2);// 输出边集


					for (Map.Entry<String, Integer> entry : vexsMap.entrySet()) {
						if (entry.getKey().equals(vex1)) { // 在点集中查找输入的第一个顶点的位置
							vex1Site = entry.getValue();
							break;
						}
					}
					for (Map.Entry<String, Integer> entry : vexsMap.entrySet()) {
						if (entry.getKey().equals(vex2)) { // 在点集中查找输入的第二个顶点的位置
							vex2Site = entry.getValue();
							break;
						}
					}
					if (this.arcs[vex1Site][vex2Site] == 0) { // 检测该边是否已经输入
						this.arcs[vex1Site][vex2Site] = 1; // 1表示该位置所对应的两顶点之间有边
						this.arcs[vex2Site][vex1Site] = 1; // 对称边也置1
					}
				}
			}
//			for(int i=0;i<length;i++){
//				for(int j=0;j<length;j++){
//					System.out.print(arcs[i][j]);
//				}
//				System.out.println();
//			}


//			Path.printLine("Edges have been initialized!");
//			Path.printLine("Algorithm recognizes Edges: " +arcs);
			workbook.close();
			io.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadNode(File file){
		try {
			// 加载点集
			InputStream io = new FileInputStream(file.getAbsoluteFile());
			XSSFWorkbook workbook = new XSSFWorkbook(io);
			Sheet sheet = workbook.getSheetAt(1);
			int number = 0;
			int firstrow = sheet.getFirstRowNum();
			int lastrow = sheet.getLastRowNum();

			for (int i = firstrow; i < lastrow + 1; i++) {
				Row row = sheet.getRow(i);
				if (row != null) {
					Cell cell = row.getCell(0);
					if (cell != null) {
						String string = cell.toString();
						vexsMap.put(string, number++);
					}
				}
			}
//			Path.printLine("Nodes have been initialized!");
//			System.out.println(vexsMap);
//			Path.printLine("Algorithm recognizes Nodes: " +vexsMap);
			workbook.close();
			io.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadAnnotation(File file){
		try {
			// 加载注释
			InputStream io = new FileInputStream(file.getAbsoluteFile());
			XSSFWorkbook workbook = new XSSFWorkbook(io);
			Sheet sheet = workbook.getSheetAt(2);
			int firstrow = sheet.getFirstRowNum();
			int lastrow = sheet.getLastRowNum();
			String name = sheet.getRow(0).getCell(0).toString();
			List<String> noteList = new ArrayList<String>();// value
			for (int i = firstrow; i < lastrow + 1; i++) {
				Row row = sheet.getRow(i);
				Notes.add(row.getCell(1).toString());
				if (!name.equals(row.getCell(0).toString())) {
					name = row.getCell(0).toString();
					noteList = new ArrayList<String>();
				}
				noteList.add(row.getCell(1).toString());
				nodeNotes.put(name, noteList);
			}
//			Path.printLine("Annotations have been initialized!");
//			Path.printLine("Annotations have been initialized!"+nodeNotes);
//			Path.printLine("Algorithm recognizes Annotations: " +nodeNotes);
			workbook.close();
			io.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadCYC(File file){
		try {
			// 加载CYC
			InputStream io = new FileInputStream(file.getAbsoluteFile());
			XSSFWorkbook workbook = new XSSFWorkbook(io);
			Sheet sheet = workbook.getSheetAt(3);//输入文件为cyc.xlsx时index为0，只有一个sheet

			int firstrow = sheet.getFirstRowNum();
			int lastrow = sheet.getLastRowNum();

			String complex = sheet.getRow(0).getCell(1).toString();
			List<String> cycList = new ArrayList<String>();

			for (int i = firstrow; i < lastrow + 1; i++) {
				Row row = sheet.getRow(i);
				if (!complex.equals(row.getCell(1).toString())) {
					complex = row.getCell(1).toString();
					CYC.add(cycList);
					cycList = new ArrayList<String>();
				}
				cycList.add(row.getCell(0).toString());
			}
			CYC.add(cycList);
//			Path.printLine("CYC has been initialized!");
//			System.out.println(CYC);
//			Path.printLine("Algorithm recognizes CYC: " +CYC);
			workbook.close();
			io.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 一阶邻居节点（度）---------------------------------------------------------------------------------------
	public void AdjacentNodes() {
		int num;
		for (int i = 0; i < vexsMap.size(); i++) {
			num = 0;
			for (int j = 0; j < vexsMap.size(); j++) {
				if (arcs[i][j] == 1) {
					num++;
				}
			}
			AdjacetNode.put(getKey(vexsMap, i), num);
		}
//		Path.printLine("The calculation of first-order neighbor nodes is completed.\n");
////		 打印一阶邻居节点
//		 for (Map.Entry<String, Integer> entry : AdjacetNode.entrySet()) {
//		 	Path.printLine("Nodes: " + entry.getKey() + "  " + "degree: " + entry.getValue());
//		 }
	}

	// 共同(交集)邻居节点------两节点的邻居节点个数取交集-----------------------------------------------------------------------
	public void CommonNodes() {

		CommonNode = new int[vexsMap.size()][vexsMap.size()];

		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {

				int n = 0;
				if (i == j) {
					CommonNode[i][j] = 0;
					continue;
				}
				for (int k = 0; k < vexsMap.size(); k++) {
					if ((arcs[i][k] == 1) && (arcs[j][k] == 1)) {
						n++;
						CommonNode[i][j] = n;
					}
				}
			}
		}
//		for(int i=0;i<vexsMap.size();i++)
//		{
//			for(int j=0;j<vexsMap.size();j++)
//			{
//				System.out.print(CommonNode[i][j]);
//			}
//			System.out.println();
//		}
//
//		Path.printLine("The calculation of intersection neighbor nodes is completed.\n");
	}

	// 并集邻居节点------------两节点的邻居节点个数取并集---------------------------------------------------------------
	public void UnionNodes() {

		UnionNode = new int[vexsMap.size()][vexsMap.size()];
		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {
				if (i == j) {
					continue;
				}
				UnionNode[i][j] = AdjacetNode.get(getKey(vexsMap, i)) + AdjacetNode.get(getKey(vexsMap, j))
						- CommonNode[i][j];
			}
		}
//		for(int i=0;i<vexsMap.size();i++)
//		{
//			for(int j=0;j<vexsMap.size();j++)
//			{
//				System.out.print(UnionNode[i][j]);
//			}
//			System.out.println();
//		}
		Path.printLine("The calculation of the union neighbor node is completed.\n");
	}

	// 拓扑结构相似性------------------------------------------------------------------------------------------
	public void T_sim() {

		t_sims = new float[vexsMap.size()][vexsMap.size()];
		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {
				if (i == j) {
					continue;
				} // 分子=|N(A)∩N(B)|=共同邻居节点 分母=|N(A)UN(B)|=并集邻居节点
				t_sims[i][j] = (float) CommonNode[i][j] / (float) UnionNode[i][j];
			}
		}
//		for(int i=0;i<vexsMap.size();i++)
//		{
//			for(int j=0;j<vexsMap.size();j++)
//			{
//				System.out.print(t_sims[i][j]+" ");
//			}
//			System.out.println();
//		}
//		Path.printLine("The topological structure similarity T_sim is calculated.\n");
	}

	// 语义相似性------------------------------------------------------------------------------------------
	public void S_sim() {
		int Max = 0;
		int MaxSite = 0;
		float fenmu = 0;
		float fenzi = 0;
		s_sims = new float[vexsMap.size()][vexsMap.size()];
//		Map<String, List<String>> nodeNotes = new HashMap<String, List<String>>();//用到的某个对象只进行了声明而没有初始化，即没有被new
		for (int i = 0; i < vexsMap.size(); i++) {// 找到所有基因中包含注释信息量最大的基因
//			System.out.println(nodeNotes!=null && "".equals(nodeNotes));
			if (nodeNotes.get(getKey(vexsMap, i)).size() > Max) {
				Max = nodeNotes.get(getKey(vexsMap, i)).size();
				MaxSite = i;
			}
		}
		for (String string : Notes) {// 得到每条注释在注释库中出现的次数 P(t)
			if (Note.containsKey(string)) {
				Note.put(string, Note.get(string).intValue() + 1);
			} else {
				Note.put(string, new Integer(1));
			}
		}
		// 得到分母
		List<String> list = nodeNotes.get(getKey(vexsMap, MaxSite));
//		System.out.println(list);
		for (int i = 0; i < list.size(); i++) {
			fenmu += IC(list.get(i));
//			System.out.println(IC(list.get(i)));
		}

		// 得到分子并计算s_sims
		List<String> listA = new ArrayList<String>();
		List<String> listB = new ArrayList<String>();
		List<String> listTmp = new ArrayList<String>();
		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {
				if (i == j) {
					continue;
				}
				fenzi = 0;
				listA = nodeNotes.get(getKey(vexsMap, i));
				listB = nodeNotes.get(getKey(vexsMap, j));
				listTmp = (List<String>) CollectionUtils.intersection(listA, listB);// 取交集

				for (int k = 0; k < listTmp.size(); k++) {
					fenzi += IC(listTmp.get(k));
					//System.out.print("i  "+i+" j "+j+listTmp+" ");
					s_sims[i][j] = fenzi / fenmu;
				}
			}
		}

//		for(int i=0;i<vexsMap.size();i++)
//		{
//			for(int j=0;j<vexsMap.size();j++)
//			{
//				System.out.print(s_sims[i][j]+" ");
//			}
//			System.out.println();
//		}
//		Path.printLine("The topological semantic similarity S_sim is calculated.\n");
	}

	// 权值------------------------------------------------------------------------------------------
	public void weight() {
		weights = new float[vexsMap.size()][vexsMap.size()];

//		for (int i = 0; i < vexsMap.size(); i++) {
//			for (int j = 0; j < vexsMap.size(); j++) {
//				weights[i][j] = 0;
//			}
//		}

		for (int i = 0; i < vexsMap.size(); i++) {
			for (int j = 0; j < vexsMap.size(); j++) {
				weights[i][j] = (s_sims[i][j] + t_sims[i][j]) / 2;
				//Path.printLine("vex1:" + vexsMap.get(i)+ "  " + "j:" + vexsMap.get(j) + " " + weights[i][j]);
				//Path.printLine("vex i:" + getKey(vexsMap,i)+ "  " + "j:" + getKey(vexsMap,j)+ " " + weights[i][j]);
//				System.out.print(weights[i][j]+" ");
			} //打印一阶邻居节点
//			System.out.println();
		}


		// for (Map.Entry<String, Integer> entry : AdjacetNode.entrySet()) {
		// 	Path.printLine("i:" + i + "  " + "j:" + j + " " + weights[i][j]);
		// }

//		Path.printLine("The calculation of weight is finished.");
//		Path.printLine("The Second step of WPC has finished!");
//		Path.printLine("----------------------------------------------------------------------------------------------------------------\n");
	}

	// WPC_1------------------------------------------------------------------------------------------------------------
	public void WPC_1() {
		float num = 0;
		int n = 0;
		List<String> CS = new ArrayList<String>();
		for (Map.Entry<String, Integer> entry : vexsMap.entrySet()) {
			CS.clear();
			CS.add(entry.getKey());
			for (int i = 0; i < vexsMap.size(); i++) {
				if (arcs[entry.getValue()][i] == 1) {
					CS.add(getKey(vexsMap, i));
				}
			}
//			System.out.println(CS);
			if (AWD(entry.getKey(), CS) != 0) {
				num += AWD(entry.getKey(), CS);
				n++;
				Matrix.put(entry.getKey(), AWD(entry.getKey(), CS));
				Th = num / n;
			}
		}
//		Path.printLine("Th: " + String.format("%-8.4f", Th));
//		Path.printLine("Seeds: ");
//		Sort(Matrix);

		Seeds.addAll(sortByValueDescending(Matrix).keySet());
		System.out.println(Seeds.size());
		System.out.println(Seeds);
//		for (Map.Entry<String, Integer> entry : seedtemp.entrySet()) {
//			Seeds.add(entry.getKey());
//		}
//		System.out.println(Matrix);

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < Seeds.size(); i++) {
			tmp.append(Seeds.get(i) + " ");
		}
//		Path.printLine(tmp.toString());//输出种子节点

//		Path.printLine("The calculation of WPC_1 has been finished!");
		Path.printLine("Seeds Size" + Seeds.size());
//		System.out.println(Seeds.size());
//		Path.printLine("Seeds"+tmp.toString());//输出种子节点
	}

	// WPC_2------------------------------------------------------------------------------------------------------------
	public void WPC_2() {
		Map<String, Integer> Rejected = new HashMap<String, Integer>();// 节点标记 标记为 1的点丧失成为种子节点权力
		// List<String> CS = new ArrayList<String>();
		List<String> Remove = new ArrayList<String>();

//		System.out.println(Seeds.size());
		for (int i = 0; i < Seeds.size(); i++) {// 初始化节点标记
			Rejected.put(Seeds.get(i), 0);
		}
//		Path.printLine("111");

		for (int i = 0; i < Seeds.size(); i++) {
			if (Rejected.get(Seeds.get(i)) == 0) {
				List<String> CS = new ArrayList<String>();
				CS.add(Seeds.get(i));

				for (int j = 0; j < Seeds.size(); j++) {// 一阶节点入集合
					if (arcs[vexsMap.get(Seeds.get(i))][vexsMap.get(Seeds.get(j))] == 1) {
						CS.add(Seeds.get(j));
						
					}
				}

				for (int j = CS.size() - 1; j >= 0; j--) { //（5）-(8)判断邻居图中的节点
					if (WR(CS.get(j), CS) < 0.2) {//加权比低于阈值的节点从邻居图中剔除，并标记为reject
						if (!Remove.contains(CS.get(j))) {
							Remove.add(CS.get(j));

						}
//						Path.printLine("444");

						Rejected.put(CS.get(j), 1);
						CS.remove(j);

					}
				}


//				for (int j = CS.size() - 1; j >= 0; j--) { //（5）-(8)判断邻居图中的节点
//					if (WD(CS.get(j), CS) < 0.2) {//WD平均值低于阈值的节点从邻居图中剔除，并标记为reject
//						if (!Remove.contains(CS.get(j))) {
//							Remove.add(CS.get(j));
//
//						}
//
//						Rejected.put(CS.get(j), 1);
//						CS.remove(j);
//
//					}
//				}

				if (CS.size() > 1) {//(9)(10)
					PC.add(CS);
					
				}
			}
		}
		for (int i = 0; i < Remove.size(); i++) {//(12)-(14)对非种子节点二次处理
			for (int j = 0; j < PC.size(); j++) {//AWD>自适应阈值，则补充到复合物中
				
				if ((AWD(Remove.get(i), PC.get(j)) > Th) && (!PC.get(j).contains(Remove.get(i)))) {
					PC.get(j).add(Remove.get(i));
					
				}
			}
		}
		System.out.println(PC.size());
		for(List<String> pc:PC )
		{
			Path.printLine(pc.toString());
		}
		System.out.println(PC.size());
//		Path.printLine("Algorithm recognizes PC: " + PC);
//		Path.printLine("The calculation of WPC_2 has been finished!");
	}

	// WPC_3重叠处理------------------------------------------------------------------------------------------------------------
	public void WPC_3() {
		List<List<String>> Remove = new ArrayList<List<String>>();
		System.out.println(PC.size());
		for (int i = 0; i < PC.size(); i++) {
			for (int j = i + 1; j < PC.size(); j++) {
				if (NA(PC.get(i), PC.get(j)) > NA_WR) {
					if (WDensity(PC.get(i)) > WDensity(PC.get(j))) {
						if (!Remove.contains(PC.get(j))) {
							Remove.add(PC.get(j));
						}
					} else {
						if (!Remove.contains(PC.get(i))) {
							Remove.add(PC.get(i));
						}
					}
				}
			}
		}
		Iterator<List<String>> iterator = PC.iterator();
		while (iterator.hasNext()){
			List<String> obj = iterator.next();
			for (int i = 0; i < Remove.size(); i++) {
				if(obj == Remove.get(i)){
					iterator.remove();
				}
			}
		}

//		for(List<String> pc:PC )
//		{
//			Path.printLine(pc.toString());
//		}
//		System.out.println(PC.size());
//		Path.printLine("The calculation of WPC is finished.\n" + "The Third step of WPC has finished!");
//		Path.printLine
//		("----------------------------------------------------------------------------------------------------------------\n");
	}

	// WPC_CYC实验结果与分析------------------------------------------------------------------------------------------------------------
	public void WPC_CYC() {//PC改为PC1，同calulate
		List<List<String>> TNtemp = new ArrayList<List<String>>();
		int TN = 0;
		int TP = 0;
		int tag = 0;
		for (int i = 0; i < PC.size(); i++) {
			tag = 0;
			for (int j = 0; j < CYC.size(); j++) {
				if (NA(PC.get(i), CYC.get(j)) >= 0.2) {
					if (!TNtemp.contains(CYC.get(j))) {
						TNtemp.add(CYC.get(j));
					}
					tag = 1;
				}
			}
			if (tag == 1) {
				TP++;
			}
		}
		TN = TNtemp.size();

//		Path.printLine("Loading CYC successful: " + CYC + "\nMatch PC and CYC, and calculate Recall, Precision, and F-measure values.");
//		Path.printLine("Loading CYC successful.\n" + "Match PC and CYC, and calculate Recall, Precision, and F-measure values.");

//		Path.printLine("TP:" + TP);
//		Path.printLine("TN:" + TN);
		Recall = (float)TN / CYC.size();
		Precision = (float)TP / PC.size();
		F_measure = (2 * Recall * Precision) / (Recall + Precision);
		F_measure = (float) (Math.floor(F_measure * 1000) / 1000);
//		Path.printLine("Recall:" + Recall);
//		Path.printLine("Precision:" + Precision);
//		Path.printLine("F_measure:" + F_measure);
//		Path.printLine("The calculation of WPC is finished.\n" + "The Forth step of WPC has finished!");
//		Path.printLine("----------------------------------------------------------------------------------------------------------------\n");
	}

	// NA(A,B)------------------------------------------------------------------------------------------------------------
	public float NA(List<String> A, List<String> B) {
		float overlapRate = 0;// 重叠率
		List<String> listTemp = new ArrayList<String>();

		listTemp = (List<String>) CollectionUtils.intersection(A, B);// 取交集
		overlapRate = ((float) listTemp.size() * (float) listTemp.size()) / ((float) A.size() * (float) B.size());
		return overlapRate;
	}

	// WDensity(G)------------------------------------------------------------------------------------------------------------
	public float WDensity(List<String> G) {
		float weightDensity = 0;// 加权稠密度
		float weight = 0;
		for (int i = 0; i < G.size(); i++)
			for (int j = i + 1; j < G.size(); j++) {
				if (arcs[vexsMap.get(G.get(i))][vexsMap.get(G.get(j))] == 1) {
					weight += weights[vexsMap.get(G.get(i))][vexsMap.get(G.get(j))];
				}
			}
		weightDensity = (2 * weight) / (float) (G.size() * (G.size() - 1));
		weightDensity = (float) (Math.round(weightDensity * 10000)) / 10000;
		return weightDensity;
	}

	// WD(node,G)只在计算WR中用到了----------------------------------------------------------------------------------------------
	public float WD(String node, List<String> G) {
		float WD = 0;// 总权值
		for (int i = 1; i < G.size(); i++) {
			WD += weights[vexsMap.get(node)][vexsMap.get(G.get(i))];
		}
		WD = (float) (Math.round(WD * 10000)) / 10000;
		return WD;
	}

	// WR(node,G)------------------------------------------------------------------------------------------------------------
	public float WR(String node, List<String> G) {
		float WR = 0;
		List<String> Gn = new ArrayList<String>();
		List<String> GnTemp = new ArrayList<String>();
		Gn.add(node);
		for (int i = 0; i < Seeds.size(); i++) {// 一阶节点入集合
			if (arcs[vexsMap.get(node)][vexsMap.get(Seeds.get(i))] == 1) {
				GnTemp.add(Seeds.get(i));
			}
		}

		for (int i = 0; i < GnTemp.size(); i++) {// 二阶节点入集合
			for (int j = 0; j < Seeds.size(); j++) {
				if ((arcs[vexsMap.get(GnTemp.get(i))][vexsMap.get(Seeds.get(j))] == 1)
						&& (!Gn.contains(Seeds.get(j)))) {
					Gn.add(Seeds.get(j));
				}
			}
		}

		WR = WD(node, G) / (WD(node, G) + WD(node, Gn));
		WR = (float) (Math.round(WR * 10000)) / 10000;
		return WR;
	}

	// getKey()------------------------------------------------------------------------------------------------------------
	public String getKey(Map<String, Integer> map, Integer value) {
		String key = "";
		for (Map.Entry<String, Integer> entry : map.entrySet()) {
			if (entry.getValue().equals(value)) {
				key = entry.getKey();
			}
		}
		return key;
	}

	// IC(t)=-log(p(t))-------------------------------------------------------------------------------------------------------
	public float IC(String t) {
//		int i = Integer.parseInt(t);
		float IC = (float) -Math.log(Note.get(t));
		return IC;
	}

	// Sort()------------------------------------------------------------------------------------------------------------------
	public void Sort(Map<String, Float> Matrix) {
//		System.out.println(Matrix);
		List<Float> seedValue = new ArrayList<Float>();
		List<String> seedTemp1 = new ArrayList<String>();
		Map<String, Integer> seedTemp2 = new HashMap<String, Integer>();

		for (Map.Entry<String, Float> entry : Matrix.entrySet()) {
			seedValue.add(entry.getValue());
		}
//		System.out.println(seedValue);
		// Collections.sort(seedValue);//升序
		Collections.sort(seedValue, Collections.reverseOrder());// 降序
//		System.out.println(seedValue);
		for (int i = 0; i < seedValue.size(); i++) {
			seedTemp1.clear();
			for (Map.Entry<String, Float> entry : Matrix.entrySet()) {
				if (seedValue.get(i).equals(entry.getValue())) {
					seedTemp1.add(entry.getKey());
				}
			}
			if (seedTemp1.size() == 1) {
				Seeds.addAll(seedTemp1);
			}
			if (seedTemp1.size() > 1) {
				for (int j = 0; j < seedTemp1.size(); j++) {
					seedTemp2.put(seedTemp1.get(j), AdjacetNode.get(seedTemp1.get(j)));
				}
				seedTemp2 = sortByValueDescending(seedTemp2);

				for (Map.Entry<String, Integer> entry : seedTemp2.entrySet()) {
					Seeds.add(entry.getKey());
				}
			}
		}
	}

	// 降序排序------------------------------------------------------------------------------------------------------------------
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValueDescending(Map<K, V> map) {
		List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>(map.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
			@Override
			public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
				int compare = (o1.getValue()).compareTo(o2.getValue());
				return -compare;
			}
		});

		Map<K, V> result = new LinkedHashMap<K, V>();
		for (Map.Entry<K, V> entry : list) {
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	// AWD()------------------------------------------------------------------------------------------------------------
	public float AWD(String node, List<String> CS) {
		float wNumer = 0;// 总权值
		float AWD = 0;// 平均加权度

		for (int i = 0; i < CS.size(); i++) {
			wNumer += weights[vexsMap.get(node)][vexsMap.get(CS.get(i))];
		}
		AWD = wNumer / (float) (CS.size() - 1);
		AWD = (float) (Math.round(AWD * 10000)) / 10000;
		return AWD;
	}
}
