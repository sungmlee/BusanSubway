import javax.swing.*;
import java.awt.*;
import java.util.*;

public class BusanSubwaySwing extends JFrame {
    static class Station {
        String name;
        int x, y;

        Station(String name, int x, int y) {
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    private final Map<String, Station> stations = new HashMap<>();
    private final Map<String, Map<String, Integer>> graph = new HashMap<>();
    private final java.util.List<Line> connections = new ArrayList<>();
    private java.util.List<Line> highlightedPath = new ArrayList<>();

    private final JPanel canvas = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            drawConnections(g);
            drawStations(g);
            drawHighlightedPath(g);
        }
    };

    private JComboBox<String> startBox, endBox;
    private JLabel statusLabel;
    private JTextArea pathDetailsArea;

    public BusanSubwaySwing() {
        setTitle("Busan Subway Visualization with Swing");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        initializeSubwayData();

        JPanel controlPanel = new JPanel();
        startBox = new JComboBox<>(stations.keySet().toArray(new String[0]));
        endBox = new JComboBox<>(stations.keySet().toArray(new String[0]));
        JButton findPathButton = new JButton("최단 경로 찾기");
        statusLabel = new JLabel("환영합니다! 출발역과 도착역을 선택하세요.");
        statusLabel.setForeground(Color.BLUE);

        pathDetailsArea = new JTextArea(8, 20); // 경로 정보를 표시할 JTextArea
        pathDetailsArea.setEditable(false); // 사용자가 편집하지 못하도록 설정
        JScrollPane scrollPane = new JScrollPane(pathDetailsArea); // 스크롤 가능한 패널 추가



        findPathButton.addActionListener(e -> {
            String start = (String) startBox.getSelectedItem();
            String end = (String) endBox.getSelectedItem();

            if (start == null || end == null || start.equals(end)) {
                statusLabel.setText("출발역과 도착역을 올바르게 선택하세요.");
                statusLabel.setForeground(Color.RED);
                return;
            }

            highlightShortestPath(start, end);
            canvas.repaint();

            int totalTime = calculatePathDetails(start, end); // 경로와 시간을 계산

            statusLabel.setText(String.format("'%s'에서 '%s'까지 최단 경로를 표시했습니다.", start, end));
            statusLabel.setForeground(Color.GREEN);
            pathDetailsArea.append(String.format("\n총 소요 시간: %d분", totalTime)); // 소요 시간 출력

        });

        controlPanel.add(new JLabel("출발: "));
        controlPanel.add(startBox);
        controlPanel.add(new JLabel("도착: "));
        controlPanel.add(endBox);
        controlPanel.add(findPathButton);

        add(controlPanel, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
        add(scrollPane, BorderLayout.EAST); // 경로 정보를 오른쪽에 표시


        canvas.setBackground(Color.WHITE);
    }

    // 경로와 소요 시간을 계산하여 화면에 출력
    private int calculatePathDetails(String start, String end) {
    java.util.List<String> path = dijkstra(start, end);
    StringBuilder pathInfo = new StringBuilder("경로:\n");
    int totalTime = 0;

    for (int i = 0; i < path.size() - 1; i++) {
        String from = path.get(i);
        String to = path.get(i + 1);
        int time = graph.get(from).get(to);

        pathInfo.append(String.format("%s -> %s (%d분)\n", from, to, time));
        totalTime += time;
    }

    pathDetailsArea.setText(pathInfo.toString()); // JTextArea에 경로 정보 표시
    return totalTime; // 총 소요 시간 반환
    }
    private void initializeSubwayData() {
        double scale = 1; // 축소 비율
    
        // 환승역 이름
        Set<String> transferStations = new HashSet<>(Arrays.asList("서면", "덕천", "미남", "동래"));
    
        // 환승역 좌표를 계산하여 통일
        Map<String, int[]> transferCoordinates = new HashMap<>();
        computeTransferCoordinates(transferStations, transferCoordinates, scale);
    
        // 부산 1호선
        String[] line1 = {
            "다대포해수욕장", "다대포항", "낫개", "신장림", "장림", "동매", "신평", "하단", "당리",
            "사하", "괴정", "대티", "서대신", "동대신", "토성", "자갈치", "남포", "중앙", "부산역",
            "초량", "부산진", "좌천", "범일", "범내골", "서면", "부전", "양정", "시청", "연산",
            "교대", "동래", "명륜", "온천장", "부산대", "장전", "구서", "두실", "남산", "범어사", "노포"
        };
        int[] line1Times = {3, 2, 3, 3, 2, 3, 3, 2, 2, 2, 3, 3, 2, 2, 2, 2, 2, 3, 2, 2, 2, 3, 3, 2, 2, 2, 2, 3, 2, 2, 3, 2, 2, 3, 3, 2, 2,2,3};
    
        int[] line1X = scaleCoordinates(new int[]{69, 71, 71, 72, 73, 88, 126, 174, 214, 268,
                                                  314, 363, 410, 459, 504, 551, 592, 615, 615, 615,
                                                  615, 615, 615, 615, 615, 615, 615, 615, 615,
                                                  615, 615, 615, 615, 615, 615, 615, 615, 615, 615,615}, scale);
    
        int[] line1Y = scaleCoordinates(new int[]{601, 630, 660, 688, 722, 745, 769, 768, 768, 764,
                                                  767, 765, 765, 765, 766, 764, 748, 718, 692, 668,
                                                  645, 622, 598, 575, 546, 502, 466, 418, 381, 343,
                                                  301, 276, 249, 224, 169, 152, 142, 116, 89, 64}, scale);
    
        addLine(line1, line1X, line1Y, line1Times, "1호선");
    
        // 부산 2호선
        String[] line2 = {
            "장산", "중동", "해운대", "동백", "벡스코", "센텀시티", "민락", "수영", "광안",
            "금련산", "남천", "경성대부경대", "대연", "못골", "지게골", "문현", "국제금융센터부산은행",
            "전포", "서면", "부암", "가야", "동의대", "개금", "냉정", "주례", "감전", "사상",
            "덕포", "모덕", "모라", "구남", "구명", "덕천", "수정", "화명", "율리", "동원", "금곡", "호포", "증산", "부산대양산캠퍼스", "남양산", "양산"
        };
        int[] line2Times = {2, 2, 3, 2, 3, 2, 2, 2, 3, 2, 2, 3, 2, 2, 2, 3, 2, 2, 2, 2, 3, 2, 3, 2, 2, 2, 3, 2, 3, 2, 2, 3, 2, 3, 2, 3, 3, 3, 2, 2, 3,3};
    
        int[] line2X = scaleCoordinates(new int[]{1148, 1121, 1072, 1027, 977, 931, 900, 887, 887, 887,
                                                  887, 884, 847, 788, 729, 689, 679, 659, 615, 579,
                                                  540, 499, 460, 420, 382, 356, 343, 343, 343, 343,
                                                  343, 343, 343, 343, 343, 343, 343, 343, 343, 343,343,343,343}, scale);
    
        int[] line2Y = scaleCoordinates(new int[]{312, 350, 350, 350, 350, 350, 364, 433, 478, 538,
                                                  591, 649, 688, 689, 686, 654, 604, 559, 546, 546,
                                                  546, 546, 546, 546, 541, 519, 490, 461, 431, 400,
                                                  369, 337, 302, 276, 251, 228, 204, 181, 158, 133,111,85,63}, scale);
    
        addLine(line2, line2X, line2Y, line2Times, "2호선");

         // 부산 3호선
         String[] line3 = {
            "수영", "망미", "배산", "물만골", "연산","거제", "종합운동장", "사직", "미남", "만덕", "남산정",
            "숙등", "덕천", "구포", "강서구청", "체육공원", "대저"
        };
        int[] line3X = scaleCoordinates(new int[]{887, 840, 753, 676, 615, 595, 571, 559, 553, 510, 456, 399, 342, 305, 263, 230,214},scale);
        int[] line3Y = scaleCoordinates(new int[]{434, 380, 380, 380, 380, 380, 375, 364, 343, 300, 300, 300, 300, 300, 300, 324,353},scale);
        int[] line3Times = {2, 2, 3, 2, 3, 2, 2, 2, 3, 2, 2, 3, 2, 2, 2, 3};

        addLine(line3, line3X, line3Y, line3Times, "3호선");
    
        // 부산 4호선
        String[] line4 = {
            "미남", "동래", "수안", "낙민", "충렬사", "명장", "서동", "금사", "반여농산물시장", "석대",
            "영산대", "반여", "재송", "센텀"
        };
        int[] line4X = scaleCoordinates(new int[]{554, 615, 670, 709, 745, 783, 820, 868, 885, 886, 886, 886, 886, 886},scale);
        int[] line4Y = scaleCoordinates(new int[]{341, 300, 300, 300, 300, 300, 300, 285, 252, 216, 178, 149, 98, 68},scale);
        int[] line4Times = {2, 2, 3, 2, 3, 2, 2, 2, 3, 2, 2, 3, 2};

        addLine(line4, line4X, line4Y, line4Times, "4호선");
        
        // 동일 좌표를 환승역에 적용
        applyTransferCoordinates(transferCoordinates);
    }
    
    private void computeTransferCoordinates(Set<String> transferStations, Map<String, int[]> transferCoordinates, double scale) {
        for (String station : transferStations) {
            int sumX = 0, sumY = 0, count = 0;
    
            // 모든 노선에서 해당 역의 좌표를 검색
            for (Line line : connections) {
                if (line.lineName.equals(station)) {
                    sumX += line.x1 + line.x2;
                    sumY += line.y1 + line.y2;
                    count += 2;
                }
            }
    
            // 좌표의 평균 계산
            if (count > 0) {
                transferCoordinates.put(station, scaleCoordinates(new int[]{sumX / count, sumY / count}, scale));
            }
        }
    }
    
    private void applyTransferCoordinates(Map<String, int[]> transferCoordinates) {
        for (Map.Entry<String, int[]> entry : transferCoordinates.entrySet()) {
            String station = entry.getKey();
            int[] coordinates = entry.getValue();
            Station stationObj = stations.get(station);
            if (stationObj != null) {
                stationObj.x = coordinates[0];
                stationObj.y = coordinates[1];
            }
        }
    }

    private int[] scaleCoordinates(int[] coordinates, double scale) {
        return Arrays.stream(coordinates).map(c -> (int) (c * scale)).toArray();
    }

    private int[] generateCoordinates(int length, int start, int increment, boolean isX) {
        int[] coordinates = new int[length];
        for (int i = 0; i < length; i++) {
            coordinates[i] = start + i * increment;
        }
        return coordinates;
    }

    private void addLine(String[] stationNames, int[] xCoordinates, int[] yCoordinates, int[] times, String lineName) {
        validateCoordinates(stationNames, xCoordinates, yCoordinates);
        
        if (times.length != stationNames.length - 1) {
            throw new IllegalArgumentException(
                String.format("Times array length mismatch: stationNames(%d), times(%d)", 
                    stationNames.length, times.length));
        }
    
        for (int i = 0; i < stationNames.length; i++) {
            addStation(stationNames[i], xCoordinates[i], yCoordinates[i]);
            if (i > 0) {
                addConnection(stationNames[i - 1], stationNames[i], times[i - 1], lineName);
            }
        }
    }

    private void addStation(String name, int x, int y) {
        stations.put(name, new Station(name, x, y));
        graph.putIfAbsent(name, new HashMap<>());
    }

    private void addConnection(String from, String to, int time, String lineName) {
        graph.get(from).put(to, time);
        graph.get(to).put(from, time);
        connections.add(new Line(stations.get(from).x, stations.get(from).y, stations.get(to).x, stations.get(to).y, lineName));
    }

    private void highlightShortestPath(String start, String end) {
        highlightedPath.clear();
        java.util.List<String> path = dijkstra(start, end);
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            String lineName = findLineName(from, to);
            highlightedPath.add(new Line(stations.get(from).x, stations.get(from).y, stations.get(to).x, stations.get(to).y, lineName));
        }
    }
    private void validateCoordinates(String[] stationNames, int[] xCoordinates, int[] yCoordinates, int[] times) {
        if (stationNames.length != xCoordinates.length || stationNames.length != yCoordinates.length || stationNames.length - 1 != times.length) {
            throw new IllegalArgumentException(
                String.format("Coordinate data mismatch: stationNames(%d), xCoordinates(%d), yCoordinates(%d), times(%d)",
                              stationNames.length, xCoordinates.length, yCoordinates.length, times.length));
        }
    }

    private java.util.List<String> dijkstra(String start, String end) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(distances::get));

        final int TRANSFER_TIME = 5; // 환승 소요 시간 (분)
        Set<String> transferStations = new HashSet<>(Arrays.asList("서면", "덕천", "미남", "동래"));

        for (String station : stations.keySet()) {
            distances.put(station, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        pq.add(start);

        while (!pq.isEmpty()) {
            String current = pq.poll();

            if (current.equals(end)) break;

            for (Map.Entry<String, Integer> neighbor : graph.get(current).entrySet()) {
                int baseTime = neighbor.getValue();
                int newDist = distances.get(current) + baseTime;
    
                // 환승역 처리
                if (transferStations.contains(current) && !current.equals(start)) {
                    newDist += TRANSFER_TIME; // 환승 시간 추가
                }
    
                if (newDist < distances.get(neighbor.getKey())) {
                    distances.put(neighbor.getKey(), newDist);
                    previous.put(neighbor.getKey(), current);
                    pq.add(neighbor.getKey());
                }
            }
        }

        java.util.List<String> path = new ArrayList<>();
        for (String at = end; at != null; at = previous.get(at)) {
            path.add(0, at);
        }
        return path;
    }

    private String findLineName(String from, String to) {
        for (Line connection : connections) {
            if ((connection.x1 == stations.get(from).x && connection.y1 == stations.get(from).y &&
                    connection.x2 == stations.get(to).x && connection.y2 == stations.get(to).y) ||
                    (connection.x2 == stations.get(from).x && connection.y2 == stations.get(from).y &&
                            connection.x1 == stations.get(to).x && connection.y1 == stations.get(to).y)) {
                return connection.lineName;
            }
        }
        return "Unknown";
    }

    private void validateCoordinates(String[] stationNames, int[] xCoordinates, int[] yCoordinates) {
        if (stationNames.length != xCoordinates.length || stationNames.length != yCoordinates.length) {
            throw new IllegalArgumentException(
                    String.format("Coordinate data mismatch: stationNames(%d), xCoordinates(%d), yCoordinates(%d)",
                            stationNames.length, xCoordinates.length, yCoordinates.length));
        }
    }

    private static class Line {
        int x1, y1, x2, y2;
        String lineName;

        Line(int x1, int y1, int x2, int y2, String lineName) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.lineName = lineName;
        }
    }

    private void drawConnections(Graphics g) {
        for (Line connection : connections) {
            g.setColor(Color.GRAY);
            g.drawLine(connection.x1, connection.y1, connection.x2, connection.y2);
        }
    }
    
    private void drawStations(Graphics g) {
        for (Station station : stations.values()) {
            g.setColor(Color.BLUE);
            g.fillOval(station.x - 5, station.y - 5, 10, 10);
            g.setColor(Color.BLACK);
            g.drawString(station.name, station.x + 10, station.y);
        }
    }
    
    private void drawHighlightedPath(Graphics g) {
        Graphics2D g2 = (Graphics2D) g; // Graphics 객체를 Graphics2D로 
        g.setColor(Color.RED);
        g2.setStroke(new BasicStroke(4)); // 선의 두께를 4로 설정 (숫자를 조정하면 두께 변경)

        for (Line line : highlightedPath) {
            g.drawLine(line.x1, line.y1, line.x2, line.y2);
        }
        g2.setStroke(new BasicStroke(1)); //  원래 두께로

    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            BusanSubwaySwing frame = new BusanSubwaySwing();
            frame.setVisible(true);
        });
    }
}