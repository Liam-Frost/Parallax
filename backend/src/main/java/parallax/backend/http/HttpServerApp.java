package parallax.backend.http;

import com.sun.net.httpserver.HttpServer;
import parallax.backend.config.AppConfig;
import parallax.backend.db.InMemoryUserRepository;
import parallax.backend.db.InMemoryVehicleRepository;
import parallax.backend.db.UserRepository;
import parallax.backend.db.VehicleRepository;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class HttpServerApp {
    public static void main(String[] args) throws IOException {
        AppConfig config = new AppConfig();
        // TODO: when SQLite persistence is ready, replace InMemoryUserRepository / InMemoryVehicleRepository
        // with SQLiteUserRepository / SQLiteVehicleRepository that implement the same interfaces.
        UserRepository userRepository = new InMemoryUserRepository();
        VehicleRepository vehicleRepository = new InMemoryVehicleRepository();

        HttpServer server = HttpServer.create(new InetSocketAddress(config.getPort()), 0);
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/auth/login", new AuthLoginHandler(userRepository, config));
        server.createContext("/api/auth/register", new AuthRegisterHandler(userRepository, config));
        server.createContext("/api/account", new AccountHandler(userRepository, vehicleRepository, config));
        server.createContext("/api/vehicles", new VehiclesHandler(vehicleRepository, userRepository, config));
        server.createContext("/api/vehicles/query-image", new PlateImageQueryHandler(vehicleRepository, config));
        server.setExecutor(Executors.newCachedThreadPool());

        System.out.println("Started Parallax backend on port " + config.getPort());
        server.start();
    }
}
