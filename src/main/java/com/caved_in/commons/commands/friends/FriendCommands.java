package com.caved_in.commons.commands.friends;

import com.caved_in.commons.Commons;
import com.caved_in.commons.Messages;
import com.caved_in.commons.commands.CommandController.CommandHandler;
import com.caved_in.commons.commands.CommandController.SubCommandHandler;
import com.caved_in.commons.commands.HelpMenus;
import com.caved_in.commons.friends.Friend;
import com.caved_in.commons.friends.FriendList;
import com.caved_in.commons.menu.HelpScreen;
import com.caved_in.commons.player.PlayerHandler;
import com.caved_in.commons.player.PlayerWrapper;
import com.caved_in.commons.sql.FriendStatus;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class FriendCommands {
	@CommandHandler(name = "friends")
	public void friendsCommand(Player player, String[] args) {
		if (args.length == 0) {
			//They issued the command '/friends' so send them a message about the help command
			PlayerHandler.sendMessage(player, Messages.PROPER_USAGE("/friends help"));
		}
	}

	@SubCommandHandler(name = "help", parent = "friends")
	public void friendsHelpCommand(Player player, String args[]) {
		int page = 1;
		//Check if the player included a page number
		if (args.length >= 2) {
			String pageNumber = args[1];
			if (StringUtils.isNumeric(pageNumber)) {
				page = Integer.parseInt(pageNumber);
			} else {
				//The player included a non-numeric page argument; Send an error saying so
				PlayerHandler.sendMessage(player, Messages.INVALID_COMMAND_USAGE("page number"));
			}
		}
		//Send the player the commands help menu
		HelpMenus.getFriendsCommandHelpScreen().sendTo(player,page,6);
	}

	@SubCommandHandler(name = "add", parent = "friends")
	public void friendsAddCommand(Player player, String[] args) {
		String playerName = player.getName();
		if (args.length >= 2) {
			//Get the players wrapped data
			PlayerWrapper playerWrapper = PlayerHandler.getData(playerName);
			//Get the players friend list
			FriendList playerFriends = playerWrapper.getFriendsList();
			//Get the name of the person they wish to add as a friend
			//TODO Check if they're adding themselves.
			String addedName = args[1];
			//Check if the player's not friends with the player they're adding
			if (!playerFriends.isFriendsWith(addedName)) {
				//TODO Check if the requested players ever played on a tunnels server before
				FriendStatus requestStatus = Commons.friendDatabase.insertFriendRequest(playerName, addedName);
				switch (requestStatus) {
					case REQUESTED:
						String playerAddedName = addedName;
						//Check if the player they requested is online
						if (PlayerHandler.isOnline(addedName)) {
							//Get the added player
							Player addedPlayer = PlayerHandler.getPlayer(addedName);
							//Get the exact name of the player
							playerAddedName = addedPlayer.getName();
							//Add a new friend to the added players friend list with an un-accepted status
							PlayerHandler.getData(addedPlayer).getFriendsList().addFriend(new Friend(addedPlayer.getName(), playerName));
							//Send the player a message saying they received a friend request
							PlayerHandler.sendMessage(addedPlayer, Messages.FRIEND_REQUEST_RECEIVED(playerName));
						}
						//Add a friend to users friend list
						playerFriends.addFriend(new Friend(playerName, playerAddedName, true));
						PlayerHandler.sendMessage(player,Messages.FRIEND_REQUEST_SENT(playerAddedName));
						break;
					case ALREADY_FRIENDS:
						PlayerHandler.sendMessage(player, Messages.FRIEND_ALREADY_EXISTS(addedName));
						break;
					case ALREADY_PENDING:
						PlayerHandler.sendMessage(player, Messages.FRIEND_REQUEST_ALREADY_EXISTS(addedName));
						break;
					default:
						break;
				}
			} else {
				PlayerHandler.sendMessage(player, Messages.FRIEND_ALREADY_EXISTS(addedName));
			}
		} else {
			PlayerHandler.sendMessage(player, Messages.INVALID_COMMAND_USAGE("player"));
		}
	}

	@SubCommandHandler(name = "remove", parent = "friends")
	public void friendsRemoveCommand(Player player, String[] args) {
		String playerName = player.getName();
		//Get the wrapped player data
		PlayerWrapper playerWrapper = PlayerHandler.getData(playerName);
		if (args.length > 1) {
			FriendList friendList = playerWrapper.getFriendsList();
			//Get the name of the person the player's removing from their friends
			String friendName = args[1];
			if (friendList.isFriendsWith(friendName)) {
				Commons.friendDatabase.deleteFriendRequest(playerName, friendName);
				if (PlayerHandler.isOnline(friendName)) {
					//Send the player a message saying they were removed from someones friends list
					PlayerHandler.sendMessage(PlayerHandler.getPlayer(friendName), Messages.FRIEND_DELETED_FROM_FRIEND(playerName));
					//Remove the friend object from the others players friend list
					PlayerHandler.getData(friendName).getFriendsList().removeFriend(playerName);
				}
				//Remove the requested name from the players friend list
				friendList.removeFriend(friendName);
				PlayerHandler.sendMessage(player, Messages.FRIEND_DELETED(friendName));
			} else {
				PlayerHandler.sendMessage(player, Messages.FRIEND_DOESNT_EXIST(friendName));
			}
		} else {
			PlayerHandler.sendMessage(player, Messages.INVALID_COMMAND_USAGE("name"));
		}
	}

	public void friendsBlockCommand(Player player, String[] args) {
		// TODO Write table in SQL for player blocks, seperate from player
	}

	@SubCommandHandler(name = "requests", parent = "friends")
	public void friendsRequestListCommand(Player player, String[] args) {
		String playerName = player.getName();
		//Get all unaccepted friends of the player
		List<Friend> friendList = PlayerHandler.getData(playerName).getFriendsList().getUnacceptedFriends();
		int page = 1;
		//Check if the player has friends
		if (friendList.size() > 0) {
			if (args.length > 1) {
				//Parse the passed page argument
				String pageNumber = args[1];
				if (StringUtils.isNumeric(pageNumber)) {
					page = Integer.parseInt(pageNumber);
				} else {
					PlayerHandler.sendMessage(player, Messages.INVALID_COMMAND_USAGE("page"));
				}
			}
			//Show the friend-requests menu
			HelpMenus.getFriendRequestsHelpScreen(friendList).sendTo(player, page, 6);
		} else {
			PlayerHandler.sendMessage(player,Messages.NO_PENDING_FRIENDS);
		}
	}

	@SubCommandHandler(name = "deny", parent = "friends")
	public void friendsDenyCommand(Player player, String[] args) {
		String playerName = player.getName();
		if (args.length > 1) {
			String denyName = args[1];
			FriendList friendList = PlayerHandler.getData(player).getFriendsList();
			if (friendList.hasRequest(denyName)) {
				Commons.friendDatabase.deleteFriendRequest(playerName, denyName);
				//Check if the player who's request was denied is online
				if (PlayerHandler.isOnline(denyName)) {
					//Get their data, and send them a message saying they were rejected
					Player deniedPlayer = PlayerHandler.getPlayer(denyName);
					PlayerHandler.sendMessage(deniedPlayer, Messages.FRIEND_DENIED_REQUEST(playerName));
					//Remove the request to the player denying from their friends list
					PlayerHandler.getData(deniedPlayer).getFriendsList().removeFriend(playerName);
				}
				friendList.removeFriend(denyName);
				PlayerHandler.sendMessage(player, Messages.FRIEND_REQUEST_DENIED(denyName));
			} else {
				PlayerHandler.sendMessage(player, Messages.FRIEND_NO_REQUEST(denyName));
			}
		} else {
			PlayerHandler.sendMessage(player, Messages.INVALID_COMMAND_USAGE("name"));
		}
	}

	@SubCommandHandler(name = "accept", parent = "friends")
	public void friendsAcceptCommand(Player player, String[] args) {
		//Get the players name and their wrapped data
		String playerName = player.getName();
		PlayerWrapper playerWrapper = PlayerHandler.getData(playerName);
		if (args.length > 1) {
			String acceptName = args[1];
			//Get the players friends list
			FriendList friendList = playerWrapper.getFriendsList();
			//Check if they have a friend request from the name they're trying to accept
			if (friendList.hasRequest(acceptName)) {
				Commons.friendDatabase.acceptFriendRequest(playerName, acceptName);
				//Check if the player they're accepting is online
				if (PlayerHandler.isOnline(acceptName)) {
					Player acceptedPlayer = PlayerHandler.getPlayer(acceptName);
					//Set the friend status to accepted and send a message
					PlayerHandler.getData(acceptedPlayer).getFriendsList().acceptFriend(playerName);
					PlayerHandler.sendMessage(player, Messages.FRIEND_REQUEST_ACCEPTED(playerName));
				}
				//Set the friend status to accepted and send a message
				friendList.acceptFriend(acceptName);
				PlayerHandler.sendMessage(player,Messages.FRIEND_ACCEPTED_REQUEST(acceptName));
			} else {
				PlayerHandler.sendMessage(player, Messages.FRIEND_NO_REQUEST(acceptName));
			}
		} else {
			PlayerHandler.sendMessage(player, Messages.INVALID_COMMAND_USAGE("name"));
		}
	}

	@SubCommandHandler(name = "list", parent = "friends")
	public void friendsListCommand(Player player, String[] args) {
		String playerName = player.getName();
		List<Friend> playerFriends = PlayerHandler.getData(playerName).getFriendsList().getFriends();
		HelpScreen friendsList = HelpMenus.getFriendsListScreen(playerFriends);
		int page = 1;
		if (args.length >= 2) {
			String pageArgument = args[1];
			if (StringUtils.isNumeric(pageArgument)) {
				page = Integer.parseInt(pageArgument);
			} else {
				PlayerHandler.sendMessage(player, Messages.INVALID_COMMAND_USAGE("page number"));
			}
		}
		friendsList.sendTo(player, page, 6);
	}
}
