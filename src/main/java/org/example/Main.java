package org.example;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import org.example.models.DataStore;
import org.example.models.Ticket;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class Main {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @SneakyThrows
    public static void main(String[] args) {
        DataStore dataStore = objectMapper.readValue(new File("tickets.json"), DataStore.class);
        String departureCity = "Владивосток";
        String arrivalCity = "Тель-Авив";

        List<Ticket> suitableTickets = searchSuitableTickets(dataStore, departureCity, arrivalCity);
        Map<String, Long> carriersAndTheirMinTravelTime = findTheMinimumTimeOfTheCarriers(suitableTickets);

        String carriersInfoTravelTime = """
                Минимальное время полета от "%s" до "%s":
                %s""".formatted(
                        departureCity,
                        arrivalCity,
                        showMinFlightTimeCarriers(carriersAndTheirMinTravelTime));

        System.out.println(carriersInfoTravelTime);

        double averagePrice = getAveragePrice(suitableTickets);
        double medianPrice = getMedianPrice(suitableTickets);

        System.out.printf("Разница между средней ценой и медианой цены: \n%s%n", averagePrice - medianPrice);
    }

    private static Map<String, Long> findTheMinimumTimeOfTheCarriers(List<Ticket> tickets) {
        Map<String, Long> carriers = new HashMap<>();
        tickets.forEach(ticket -> {
            String carrier = ticket.getCarrier();
            long flightTime = calculateFlightTime(ticket).toMinutes();

            if (carriers.containsKey(carrier)) {
                if (carriers.get(carrier) > flightTime)
                    carriers.put(carrier, flightTime);
            } else
                carriers.put(ticket.getCarrier(), flightTime);
        });

        return carriers;
    }

    private static double getAveragePrice(List<Ticket> tickets) {
        return tickets.stream().mapToDouble(Ticket::getPrice).sum() / tickets.size();
    }

    private static double getMedianPrice(List<Ticket> tickets) {
        double medianPrice;
        int index;
        List<Ticket> sortTicketsByPrice = new ArrayList<>(tickets).stream()
                .sorted(Comparator.comparingInt(Ticket::getPrice))
                .toList();

        if (sortTicketsByPrice.size() % 2 == 0) {
            index = (sortTicketsByPrice.size()/2 + sortTicketsByPrice.size()/2 -1) / 2;
            double a = sortTicketsByPrice.get(index).getPrice();
            double b = sortTicketsByPrice.get(index +1).getPrice();
            medianPrice = (a + b) / 2;
        }
        else {
            index =  sortTicketsByPrice.size() / 2;
            medianPrice = sortTicketsByPrice.get(index).getPrice();
        }

        return medianPrice;
    }

    private static List<Ticket> searchSuitableTickets(DataStore dataStore,
                                                      String departureCity,
                                                      String arrivalCity) {
        return dataStore.getTickets().stream()
                .filter(ticket ->
                        ticket.getOriginName().equals(departureCity) && ticket.getDestinationName().equals(arrivalCity))
                .toList();
    }

    private static Duration calculateFlightTime(Ticket ticket) {
        LocalDateTime departure = LocalDateTime.of(ticket.getDepartureDate(), ticket.getDepartureTime());
        LocalDateTime arrival = LocalDateTime.of(ticket.getArrivalDate(), ticket.getArrivalTime());

        return Duration.between(departure, arrival);
    }

    private static String showMinFlightTimeCarriers(Map<String, Long> carriers) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Long> map : carriers.entrySet()) {
            stringBuilder
                    .append("Перевозчик %s: %dч %dм".formatted(
                            map.getKey(),
                            map.getValue() / 60,
                            map.getValue() % 60))
                    .append("\n");
        }
        return stringBuilder.toString();
    }
}