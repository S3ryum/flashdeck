import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

public final class FlashDeck {
    private static final Path DATA_FILE = Path.of("flashdeck.tsv");
    private static final int[] INTERVALS = {1, 3, 7, 14, 30};
    private static final class Card {
        long id; String front; String back; LocalDate due; int streak;
        Card(long id, String front, String back, LocalDate due, int streak) {
            this.id = id; this.front = front; this.back = back; this.due = due; this.streak = streak;
        }
    }
    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
    private static List<Card> load() throws IOException {
        List<Card> cards = new ArrayList<>();
        if (!Files.exists(DATA_FILE)) return cards;
        int lineNumber = 0;
        for (String line : Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8)) {
            lineNumber++;
            if (line.isBlank()) continue;
            String[] fields = line.split("\\|", -1);
            if (fields.length != 5) throw new IOException("Invalid data at line " + lineNumber + ".");
            cards.add(new Card(Long.parseLong(fields[0]), decode(fields[1]), decode(fields[2]),
                    LocalDate.parse(fields[3]), Integer.parseInt(fields[4])));
        }
        return cards;
    }
    private static void save(List<Card> cards) throws IOException {
        List<String> lines = cards.stream().map(card -> card.id + "|" + encode(card.front) + "|"
                + encode(card.back) + "|" + card.due + "|" + card.streak).toList();
        Path temporary = DATA_FILE.resolveSibling("flashdeck.tsv.tmp");
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        Files.move(temporary, DATA_FILE, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
    private static void usage() {
        System.out.println("FlashDeck - local spaced-review cards");
        System.out.println("  add <question> <answer>");
        System.out.println("  list");
        System.out.println("  study");
    }
    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("help")) { usage(); return; }
        try {
            List<Card> cards = load();
            if (args[0].equals("add")) {
                if (args.length < 3) throw new IllegalArgumentException("Usage: add <question> <answer>");
                String front = args[1].trim();
                String back = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)).trim();
                if (front.isEmpty() || back.isEmpty()) throw new IllegalArgumentException("Question and answer cannot be blank.");
                long id = cards.stream().mapToLong(card -> card.id).max().orElse(0L) + 1L;
                cards.add(new Card(id, front, back, LocalDate.now(), 0));
                save(cards);
                System.out.println("Added card " + id + "; it is ready to study today.");
                return;
            }
            if (args[0].equals("list")) {
                if (cards.isEmpty()) System.out.println("No cards yet. Add one with: add <question> <answer>");
                cards.stream().sorted(Comparator.comparing(card -> card.due))
                        .forEach(card -> System.out.println(card.id + ". " + card.front + " | due " + card.due));
                return;
            }
            if (args[0].equals("study")) {
                LocalDate today = LocalDate.now();
                Card card = cards.stream().filter(item -> !item.due.isAfter(today))
                        .min(Comparator.comparing(item -> item.due)).orElse(null);
                if (card == null) { System.out.println("Nothing is due today. Add a card or come back later."); return; }
                Scanner scanner = new Scanner(System.in);
                System.out.println("Question: " + card.front);
                System.out.println("Press Enter when you are ready to reveal the answer.");
                scanner.nextLine();
                System.out.println("Answer: " + card.back);
                System.out.print("Rate your recall (again/good): ");
                String rating = scanner.nextLine().trim().toLowerCase();
                if (rating.equals("again")) {
                    card.streak = 0; card.due = today.plusDays(1);
                } else if (rating.equals("good")) {
                    card.streak = Math.min(card.streak + 1, INTERVALS.length);
                    card.due = today.plusDays(INTERVALS[card.streak - 1]);
                } else {
                    throw new IllegalArgumentException("Rating must be again or good; card was not changed.");
                }
                save(cards);
                System.out.println("Next review: " + card.due);
                return;
            }
            usage();
            throw new IllegalArgumentException("Unknown command: " + args[0]);
        } catch (Exception error) {
            System.err.println("Error: " + error.getMessage());
            System.exit(1);
        }
    }
}
