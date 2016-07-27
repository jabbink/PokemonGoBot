import os
import time
import threading
from tkinter import *
import tkinter as tk
from tkinter import ttk
import tkinter.messagebox
import webbrowser
fileDir = os.path.dirname(os.path.realpath('__file__'))
file_name = os.path.join(fileDir, 'config.properties.template')
file_name_2 = os.path.join(fileDir, 'config.properties')
content = []
word = []
val = []
name = []
entries = []
saved_lines = []
done = False
tf = ["true", "false"]
cRow = 0
cCol = 0
firstRun = True
has_reset = False
alive = True
groupLogin = ["Username", "Password", "Base64 Password", "Token"]
groupLocation = ["Latitude", "Longitude", "Walk Only", "Speed", "Random Next Pokestop Selection"]
groupItems = ["Drop Items", "Item Revive", "Item Max Revive", "Item Potion", "Item Super Potion", "Item Hyper Potion", "Item Max Potion", "Item Poke Ball", "Item Great Ball", "Item Ultra Ball", "Item Master Ball", "Item Razz Berry", "Preferred Ball"]
groupTransfer = ["Autotransfer", "Keep Pokemon Amount", "Sort By Iv", "Transfer Iv Threshold", "Transfer Cp Threshold", "Ignored Pokemon", "Obligatory Transfer", "Desired Catch Probability", "Desired Catch Probability Unwanted"]
groupLog = ["Display Keepalive", "Display Nearest Unused", "Display Pokestop Rewards", "Display Pokemon Catch Rewards", "Display Pokestop Name"]
groupGui = ["Gui Port", "Gui Port Socket"]
groupJobs = ["Loot Pokestop", "Catch Pokemon", "Auto Fill Incubator", "Always Curve", "Never Use Berries"]
loginRow = 0
locationRow = 0
itemsRow = 0
transferRow = 0
logRow = 0
noneRow = 0
guiRow = 0
jobsRow = 0


class Entry:
    def __init__(self, text, value):
        global jobsRow
        global frameNone
        global loginRow
        global loginRow
        global locationRow
        global itemsRow
        global transferRow
        global logRow
        global noneRow
        global guiRow
        self.text = text
        self.val = value
        if self.text in groupLogin:
            self.parent = frameLogin
            loginRow += 1
            self.row = loginRow
            self.col = 0
        elif self.text in groupLocation:
            self.parent = frameLocation
            locationRow += 1
            self.row = locationRow
            self.col = 0
        elif self.text in groupItems:
            self.parent = frameItems
            itemsRow += 1
            self.row = itemsRow
            self.col = 0
        elif self.text in groupTransfer:
            self.parent = frameTransfer
            transferRow += 1
            self.row = transferRow
            self.col = 1
        elif self.text in groupLog:
            self.parent = frameLog
            logRow += 1
            self.row = logRow
            self.col = 1
        elif self.text in groupGui:
            self.parent = frameGui
            guiRow += 1
            self.row = guiRow
            self.col = 1
        elif self.text in groupJobs:
            self.parent = frameJobs
            jobsRow += 1
            self.row = jobsRow
            self.col = 1
        else:
            try:
                frameNone
            except NameError:
                frameNone = ttk.LabelFrame(root, text="Unknown")
                frameNone.grid(column=3, row=0, rowspan=2)
            self.parent = frameNone
            noneRow += 1
            self.row = noneRow
            self.col = 0
        self.label = ttk.Label(self.parent, text=self.text)
        self.label.grid(row=self.row, column=self.col, sticky="nesw")
        if self.val not in tf:
            self.entry = ttk.Entry(self.parent)
            self.entry.grid(row=self.row, column=(self.col + 1))
            self.entry.insert(END, self.val)
        if self.val in tf:
            self.var = StringVar(self.parent)
            self.var.set(value)
            if self.val == "true":
                self.entry = ttk.OptionMenu(self.parent, self.var, "true", *tf)
            if self.val == "false":
                self.entry = ttk.OptionMenu(self.parent, self.var, "false", *tf)
            self.entry.grid(row=self.row, column=(self.col + 1), sticky="nesw")
        self.entry.columnconfigure(1, weight=1)
        self.entry.grid_rowconfigure(0, weight=1)


def save():
    cfg_file = open(file_name_2, "w")
    cfg_file.truncate()
    for box in entries:
        try:
            del box_text[:]
        except:
            pass
        box_text = []
        for char in globals()[box].text:
            if char == " ":
                char = "_"
            box_text.append(char)
        things = "".join(box_text).lower()
        things += "="
        try:
            things += globals()[box].entry.get()
        except AttributeError:
            things += globals()[box].var.get()
        cfg_file.write(things)
        cfg_file.write("\n")


def reset(ask):
    global default_file
    global has_reset
    if not ask:
        try:
            default_file = open(file_name, "r")
            cfg_file = open(file_name_2, "w")
            cfg_file.truncate()
            for default_line in default_file:
                cfg_file.write(default_line)
            tkinter.messagebox.showinfo("Created file", "Created config.properties from defaults, when you are done edi"
                                                        "ting values press save.\nIf you need to reset to defaults, pre"
                                                        "ss reset")
        except FileNotFoundError:
            tkinter.messagebox.showerror("Missing file!", "Could not find config.properties.template, please ensure "
                                                          "that it is next to this file")
    if ask:
        if tkinter.messagebox.askokcancel("Reset", "Are you sure you want to reset to defaults?"):
            default_file = open(file_name, "r")
            cfg_file = open(file_name_2, "w")
            cfg_file.truncate()
            for default_line in default_file:
                if default_line.find("#") == 0 or not default_line.strip():
                    pass
                else:
                    cfg_file.write(default_line)
            has_reset = True


def make_entries():
    lines = 0
    # noinspection PyGlobalUndefined
    global reset_button
    # noinspection PyGlobalUndefined
    global save_button
    global done
    global firstRun
    global cRow
    global cCol
    with open(file_name_2) as file:
        for line in file:
            if line.find("#") == 0 or not line.strip():
                pass
            else:
                lines += 1
    with open(file_name_2) as file:
        for line in file:
            if line.find("#") == 0 or not line.strip():
                pass
            else:
                if not firstRun:
                    val.pop(-1)
                    globals()["".join(name)] = Entry("".join(name), "".join(val))
                    entries.append("".join(name))
                    del name[:]
                    del val[:]
                firstRun = False
                mode = 1
                next_capital = True
                for char in line:
                    if mode == 1:
                        if char in ["="]:
                            mode = 2
                        else:
                            if next_capital:
                                char = char.upper()
                                next_capital = False
                            if char == "_":
                                char = " "
                                next_capital = True
                            name.append(char)
                    if mode == 2:
                        if char not in ["=", ""]:
                            val.append(char)
        done = True
        cRow += 1
        val.pop(-1)
        globals()["".join(name)] = Entry("".join(name), "".join(val))
        entries.append("".join(name))
        del name[:]
        del val[:]
        save_button = ttk.Button(frameTools, text="Save", command=save)
        save_button.grid(row=5, column=1)
        reset_button = ttk.Button(frameTools, text="Reset", command=lambda: reset(True))
        reset_button.grid(row=5, column=2)


def callback1(args):
    webbrowser.open_new(r"https://github.com/jabbink/PokemonGoBot")
    return args


def callback2(*args):
    webbrowser.open_new(r"https://github.com/ZingBallyhoo")
    return args


def display_about():
    t = tk.Toplevel()
    t.resizable(width=False, height=False)
    t.wm_title("About")
    l = tk.Label(t, text="Made by ZingBallyhoo")
    l.pack(side="top", fill="both", expand=True, padx=40, pady=25)
    link = Label(t, text="Pokemon Go Bot Repo", fg="blue", cursor="hand2")
    link.pack()
    link.bind("<Button-1>", callback1)
    link2 = Label(t, text="Config tool Repo", fg="blue", cursor="hand2")
    link2.pack()
    link2.bind("<Button-1>", callback2)
    l = tk.Label(t, text="Made using Python 3.5.2")
    l.pack(side="top", fill="both", expand=True)
    t.focus()
template_lines = []
properties_lines = []
current_line = []
current_val = []


def thread_loop():
    global done
    global cRow
    global cCol
    global firstRun
    global has_reset
    while True:
        try:
            make_entries()
        except FileNotFoundError:
            open(file_name_2, 'w')
            reset(False)
        if done:
            break
    while alive:
        if has_reset:
            for box in entries:
                globals()[box].entry.grid_forget()
                globals()[box].label.grid_forget()
            del entries[:]
            done = False
            cRow = 0
            cCol = 0
            firstRun = True
            has_reset = False
            save_button.grid_forget()
            reset_button.grid_forget()
            make_entries()
try:
    default_file = open(file_name, "r")
except FileNotFoundError:
    tkinter.messagebox.showerror("Missing file!", "Could not find config.properties.template, please ensure that"
                                                  " it is next to this file")
    alive = False


def set_theme(new_theme):
    print(new_theme)
    s.theme_use(new_theme)
if alive:
    root = tk.Tk()
    root.resizable(width=False, height=False)
    root.title("Pokemon GO Bot Configuration Tool")
    frameLogin = ttk.LabelFrame(root, text="Login")
    frameLogin.grid(rowspan=1, sticky="ewns", columnspan=2)
    frameLocation = ttk.LabelFrame(root, text="Location")
    frameLocation.grid(rowspan=1, sticky="ewns", columnspan=2)
    frameItems = ttk.LabelFrame(root, text="Items")
    frameItems.grid(rowspan=3, sticky="ewns", columnspan=2)
    frameTransfer = ttk.LabelFrame(root, text="Transfer")
    frameTransfer.grid(column=2, rowspan=2, row=0, columnspan=2, sticky="ewns")
    frameLog = ttk.LabelFrame(root, text="Logging")
    frameLog.grid(column=2, row=2, rowspan=1, columnspan=2, sticky="ewns")
    frameGui = ttk.LabelFrame(root, text="Web GUI")
    frameGui.grid(column=2, row=3, columnspan=2, sticky="ewns")
    frameJobs = ttk.LabelFrame(root, text="Jobs")
    frameJobs.grid(column=2, row=4, columnspan=2, sticky="ewns")
    frameTools = ttk.LabelFrame(root, text="Tools")
    frameTools.grid(column=0, row=5, columnspan=4, sticky="ewns")
    menu_bar = Menu(root)
    s = ttk.Style()
    print(s.theme_names())
    themeMenu = Menu(menu_bar, tearoff=0)
    try:
        s.theme_use('vista')
        win = 1
    except tkinter.TclError:
        s.theme_use('clam')
        win = 0
    if win == 1:
        themeMenu.add_command(label="winnative", command=lambda: set_theme("winnative"))
        themeMenu.add_command(label="vista", command=lambda: set_theme("vista"))
        themeMenu.add_command(label="xpnative", command=lambda: set_theme("xpnative"))
    themeMenu.add_command(label="clam", command=lambda: set_theme("clam"))
    themeMenu.add_command(label="alt", command=lambda: set_theme("alt"))
    themeMenu.add_command(label="default", command=lambda: set_theme("default"))
    themeMenu.add_command(label="classic", command=lambda: set_theme("classic"))
    menu_bar.add_cascade(label="About", command=display_about)
    menu_bar.add_cascade(label="Theme", menu=themeMenu)
    root.config(menu=menu_bar)
    for x in range(60):
        Grid.columnconfigure(root, x, weight=1)
    for y in range(30):
        Grid.rowconfigure(root, y, weight=1)
    loop = threading.Thread(target=thread_loop)
    loop.daemon = True
    loop.start()
    mainloop()
    alive = False
