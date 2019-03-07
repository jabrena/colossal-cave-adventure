
/*
 * File: Adventure.java
 * --------------------
 * This program plays the Adventure game.
 */

import java.io.*;
import java.util.*;

/* Class: Adventure */
/**
 * This class is the main program class for the Adventure game.
 */

public class Adventure {
	
	// game state
	private boolean play;
	// adventure map
	private SortedMap<Integer, AdvRoom> map = new TreeMap<Integer, AdvRoom>();
	// objects in the adventure
	private Map<String, AdvObject> objects = new HashMap<String, AdvObject>();
	// available commands
	private Map<String, AdvCommand> commands = new HashMap<String, AdvCommand>();
	// commands' synonyms
	private Map<String, String> synonyms = new HashMap<String, String>();
	// player's inventory
	private ArrayList<AdvObject> inventory = new ArrayList<AdvObject>();
	// player's location
	private AdvRoom playerRoom;
	// player input scanner
	private static Scanner scan = new Scanner(System.in);

	/**
	 * This method is used only to test the program
	 */
	public static void setScanner(Scanner theScanner) {
		scan = theScanner;
	}

	/**
	 * Runs the adventure program
	 */
	public static void main(String[] args) {
//		while (true) {
//			
//		}
		System.out.print("What will be your adventure today? ");
		String input = scan.nextLine();
		Adventure game = new Adventure();
		if (game.load(input)) {
			game.run();
		}
	}
	
	/**
	 * Loads the adventure and the corresponding files
	 * @param name
	 * 			The name of the adventure
	 * @return
	 * 		True if loading was successful, false if it failed.
	 */
	private boolean load(String name) {
		Scanner scan;
		File file;
		
		// read the "rooms" file
		try {
			file = new File(name + "Rooms.txt");
			scan = new Scanner(file);

			while (scan.hasNext()) {
				AdvRoom room = AdvRoom.readFromFile(scan);
				map.put(room.getRoomNumber(), room);
			}
			scan.close();
		} catch (IOException e) {
			System.out.println("Problem reading " + name + "Rooms.txt" + ": " + e);
			return false;
		}
		
		// read the "objects" file if it exists
		try {
			file = new File(name + "Objects.txt");
			if (file.exists()) {
				scan = new Scanner(file);
				while (scan.hasNext()) {
					AdvObject object = AdvObject.readFromFile(scan);
					map.get(object.getInitialLocation()).addObject(object);
					objects.put(object.getName(), object);
				}
				scan.close();
			}
		} catch (IOException e) {
			System.out.println("Problem reading " + name + "Objects.txt" + ": " + e);
			return false;
		}

		// read the "synonyms" file if it exists
		try {
			file = new File(name + "Synonyms.txt");
			if (file.exists()) {
				scan = new Scanner(file);
				String line;
				while (scan.hasNextLine() && (line = scan.nextLine()).trim().length() > 0) {
					String[] split = line.split("=");
					synonyms.put(split[0].toUpperCase(), split[1].toUpperCase());
				}
				scan.close();
			}
		} catch (IOException e) {
			System.out.println("Problem reading " + name + "Synonyms.txt" + ": " + e);
			return false;
		}
		
		// add available commands
		commands.put("DROP", new DropCommand());
		commands.put("HELP", new HelpCommand());
		commands.put("INVENTORY", new InventoryCommand());
		commands.put("LOOK", new LookCommand());
		commands.put("TAKE", new TakeCommand());
		commands.put("QUIT", new QuitCommand());
		
		for (Integer i : map.keySet()) {
			AdvRoom room = map.get(i);
			for (AdvMotionTableEntry entry : room.getMotionTable()) {
				commands.put(entry.getDirection(), new AdvMotionCommand(entry.getDirection()));
			}
		}

		return true;
	}
	
	/**
	 * Reads the input from the player
	 * and takes appropriate actions.
	 */
    public void run() {
    	// starts the adventure by going into the first room
    	// and printing the long description
        playerRoom = map.get(map.firstKey());
        System.out.print("\n> ");
        executeLookCommand();
        playerRoom.setVisited(true);
        play = true;
        
        // executes while the game state is true
        while (play) {
			System.out.print("> ");
			String input = scan.nextLine().trim().toUpperCase();

			if (input.length() > 0) {
				String[] command = input.split("\\s+");

				// replaces synonyms
				for (int i = 0; i < command.length; i++) {
					if (synonyms.containsKey(command[i])) {
						command[i] = synonyms.get(command[i]);
					}
				}

				// executes the command
				if (commands.containsKey(command[0])) {
					if (command.length == 1) {
						commands.get(command[0]).execute(this, null);
						continue;
					} else if (command.length == 2 && objects.containsKey(command[1])) {
						commands.get(command[0]).execute(this, objects.get(command[1]));
						continue;
					}
				}
				System.out.println("Unavailable command");
			}
        }
    }
	
	/* Method: executeMotionCommand(direction) */
	/**
	 * Executes a motion command. This method is called from the
	 * AdvMotionCommand class to move to a new room.
	 * 
	 * @param direction
	 *            The string indicating the direction of motion
	 */
    public void executeMotionCommand(String direction) {
    	boolean found = false;
    	for (AdvMotionTableEntry entry : playerRoom.getMotionTable()) {
    		if (entry.getDirection().equals(direction) && inventory.contains(objects.get(entry.getKeyName()))) {
    			playerRoom = map.get(entry.getDestinationRoom());
    			found = true;
    			break;
    		} else if (entry.getDirection().equals(direction) && entry.getKeyName() == null) {
    			playerRoom = map.get(entry.getDestinationRoom());
    			found = true;
    			break;
    		}
    	}

    	if (found) {
            if (playerRoom == null) {
                play = false;
            } else {
                if (playerRoom.hasBeenVisited()) {
                    System.out.println(playerRoom.getName());
                } else {
                    executeLookCommand();
                }
                
                if (playerRoom.getMotionTable()[0].getDirection().equals("FORCED")) {
                	executeMotionCommand("FORCED");
                }
                else {
                	playerRoom.setVisited(true);
                }
            }
        } else {
            System.out.println("Unavailable direction");
        }
    }

	/* Method: executeQuitCommand() */
	/**
	 * Implements the QUIT command. This command should ask the user to confirm
	 * the quit request and, if so, should exit from the play method. If not,
	 * the program should continue as usual.
	 */
	public void executeQuitCommand() {
        System.out.print("Confirm quit, your progress will not be saved (Y/N)? ");
        String input = scan.nextLine().trim().toUpperCase();
        play = !input.equals("Y");
	}

	/* Method: executeHelpCommand() */
	/**
	 * Implements the HELP command. Your code must include some help text for
	 * the user.
	 */
	public void executeHelpCommand() {
		System.out.println("List of all possible commands: "
				+ "\nDROP"
				+ "\nHELP"
				+ "\nINVENTORY"
				+ "\nLOOK"
				+ "\nTAKE"
				+ "\nQUIT");

	}

	/* Method: executeLookCommand() */
	/**
	 * Implements the LOOK command. This method should give the full description
	 * of the room and its contents.
	 */
	public void executeLookCommand() {
		for (String line : playerRoom.getDescription()) {
			System.out.println(line);
		}
	}

	/* Method: executeInventoryCommand() */
	/**
	 * Implements the INVENTORY command. This method should display a list of
	 * what the user is carrying.
	 */
	public void executeInventoryCommand() {
		if (inventory.size() > 0) {
			for (AdvObject obj : inventory) {
				System.out.println(obj.getName() + ": " + obj.getDescription());
			}
		} else {
			System.out.println("Your inventory is empty");
		}
	}

	/* Method: executeTakeCommand(obj) */
	/**
	 * Implements the TAKE command. This method should check that the object is
	 * in the room and deliver a suitable message if not.
	 * 
	 * @param obj
	 *            The AdvObject you want to take
	 */
	public void executeTakeCommand(AdvObject obj) {
		if (playerRoom.containsObject(obj)) {
			inventory.add(obj);
			playerRoom.removeObject(obj);
			System.out.println("Taken");
		} else if (obj != null) {
			System.out.println("You don't see any " + obj.getName());
		} else {
			System.out.println("There is nothing to take");
		}
	}

	/* Method: executeDropCommand(obj) */
	/**
	 * Implements the DROP command. This method should check that the user is
	 * carrying the object and deliver a suitable message if not.
	 * 
	 * @param obj
	 *            The AdvObject you want to drop
	 */
	public void executeDropCommand(AdvObject obj) {
		if (inventory.contains((Object) obj)) {
			inventory.remove((Object) obj);
			playerRoom.addObject(obj);
			System.out.println("Dropped");
		} else if (obj != null) {
			System.out.println("You don't have any " + obj.getName() + " to drop");
		} else {
			System.out.println("You don't have that object");
		}
	}
}
