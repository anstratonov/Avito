package sample;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import sample.api.AvitoAd;
import sample.dbclasses.Category;
import sample.dbclasses.JDBCClient;
import sample.models.Filter;
import sample.parse.Parse;
import sample.services.AvitoAdsService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

public class Main extends Application {

    public static JDBCClient jdbcClient;
    public static HashMap<String, String> citys;
    public static HashMap<String, String> categories_;
    public static HashMap<String, String> subcategories_;
    public static ArrayList<Category> categories;

    private static String URL = "https://www.avito.ru/map";
    private static String CitiesURL = "https://www.avito.ru/";

    private static String mainUrl = "http://www.avito.ru";

    static final String DB_URL = "jdbc:postgresql://localhost:5432/avitodb";
    static final String LOGIN = "postgres";
    static final String PASSWORD = "10041994";

    public static Filter filter = new Filter("rossiya", 0, 0, true, "transport");
    public static ObservableList<AvitoAd> adsObservableList = FXCollections.observableArrayList();
    private  static AvitoAdsService avitoAdsService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        avitoAdsService.start();

        Parent root = FXMLLoader.load(getClass().getResource("/sample/view/filter.fxml"));
        primaryStage.setTitle("filter");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }

    public static void main(String[] args) {
//        parseCategories();
        avitoAdsService = new AvitoAdsService(filter, null, null);
        avitoAdsService.setPeriod(Duration.seconds(20));
        avitoAdsService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent event) {
                System.out.println("Service refreshed data");
                try {
                    adsObservableList.addAll(avitoAdsService.getValue());
                    adsObservableList.sort(new Comparator<AvitoAd>() {
                        //более новые в начале
                        @Override
                        public int compare(AvitoAd ad1, AvitoAd ad2) {
                            return ad1.getDateTime().compareTo(ad2.getDateTime()) * (-1);
                        }
                    });
                } catch (NullPointerException e) {

                }
            }
        });

        try {
            jdbcClient = new JDBCClient();
        } catch (ClassNotFoundException e) {
            System.out.println(e.getException());
        }
        if(jdbcClient.isTable("category"))
            Parse.parseCategories(jdbcClient);
        loadCities();
        loadCategories();
        launch(args);
    }

    private static void loadCategories() {
        categories_ = new HashMap<String, String>();
        subcategories_ = new HashMap<String, String>();
        try {
            for (Category item : jdbcClient.categorySelectParent()) {
                categories_.put(item.getName(), item.getURL());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void loadCities() {
        citys = new HashMap<String, String>();
        try {
            Document doc = Jsoup.connect(CitiesURL).get();
            Elements cities = doc.select("div.col-2");
            //System.out.println("\t\t" + cities);
            //Elements _cities = cities.select("cities");

            for (org.jsoup.nodes.Element city : cities) {
                Elements city_ = city.select("*");
                for (org.jsoup.nodes.Element _city : city_) {
                    org.jsoup.nodes.Element links = _city.select("a").first();
                    String linkHref = links.attr("href");
                    String linkInnerH = links.html();
                    citys.put(linkInnerH, linkHref.substring(15));
                }
            }
            citys.put("По всей России", "rossiya");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
