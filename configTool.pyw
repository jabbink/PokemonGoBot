import os
import time
import threading
from tkinter import *
import tkinter as tk
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
groupLocation = ["Latitude", "Longitude", "Walk Only", "Speed"]
groupItems = ["Drop Items", "Item Revive", "Item Max Revive", "Item Potion", "Item Super Potion", "Item Hyper Potion", "Item Max Potion", "Item Poke Ball", "Item Great Ball", "Item Ultra Ball", "Item Master Ball", "Item Razz Berry", "Preferred Ball"]
groupTransfer = ["Autotransfer", "Keep Pokemon Amount", "Sort By Iv", "Transfer Iv Threshold", "Transfer Cp Threshold", "Ignored Pokemon", "Obligatory Transfer", "Desired Catch Probability"]
groupLog = ["Display Keepalive", "Display Nearest Unused", "Display Pokestop Rewards", "Display Pokemon Catch Rewards", "Display Pokestop Name"]
groupGui = ["Gui Port", "Gui Port Socket"]
loginRow = 0
locationRow = 0
itemsRow = 0
transferRow = 0
logRow = 0
noneRow = 0
guiRow = 0


class Entry:
    def __init__(self, text, value):
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
        else:
            try:
                frameNone
            except NameError:
                frameNone = LabelFrame(root, text="Unknown")
                frameNone.grid(column=3, row=0)
            self.parent = frameNone
            noneRow += 1
            self.row = noneRow
            self.col = 0
        self.label = tk.Label(self.parent, text=self.text)
        self.label.grid(row=self.row, column=self.col)
        if value not in tf:
            self.entry = tk.Entry(self.parent)
            self.entry.grid(row=self.row, column=(self.col + 1))
            self.entry.insert(END, self.val)
        if value in tf:
            self.var = tk.StringVar(self.parent)
            self.var.set(value)
            self.entry = OptionMenu(self.parent, self.var, *tf)
            self.entry.grid(row=self.row, column=(self.col + 1), sticky="ew")
            # self.entry.insert(END, self.val)


def save():
    cfg_file = open(file_name_2, "w")
    cfg_file.truncate()
    for box in entries:
        things = globals()[box].text
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
            print("hi")
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
        globals()["".join(name)] = Entry("".join(name), "".join(val))
        entries.append("".join(name))
        del name[:]
        del val[:]
        save_button = tk.Button(None, text="Save", command=save)
        save_button.grid(row=5, column=1)
        reset_button = tk.Button(None, text="Reset", command=lambda: reset(True))
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
template_lines = []
properties_lines = []
current_line = []
current_val = []

'''def check_template():
    first_pass = True
    with open(file_name) as file:
        for line in file:
            if line.find("#") == 0 or not line.strip():
                pass
            else:
                if not first_pass:
                    # dict = {'Name': 'Zara', 'Age': 7, 'Class': 'First'}
                    dict = {}
                    dict["".join(current_line)] = "".join(current_val)
                    print(dict)
                    # Add new entry
                    del current_val[:]
                    del current_line[:]
                doIt = 1
                first_pass = False
                for char in line:
                    if doIt == 1:
                        if char in ["="]:
                            doIt = 2
                        else:
                            current_line.append(char)
                    if doIt == 2:
                        if char != "=":
                            current_val.append(char)
                            print(template_lines)
    with open(file_name_2) as file:
        for line in file:
            if line.find("#") == 0 or not line.strip():
                pass
            else:
                doIt = 1
                for char in line:
                    if doIt == 1:
                        if char in ["="]:
                            doIt = 2
                        else:
                            current_line.append(char)
                    if doIt == 2:
                        properties_lines.append("".join(current_line))
                        doIt = 0
                        print(properties_lines)
                        del current_line[:]
    for item in template_lines:
        with open(file_name_2) as file:
            saved_lines = []
            for line in file:
                saved_lines.append(line)
        if item not in properties_lines:
            saved_lines.append(item)
    # print(saved_lines)
    cfg_file = open(file_name_2, "w")
    cfg_file.truncate()
    for line in saved_lines:
        cfg_file.write(line)
    for item in properties_lines:
        with open(file_name_2) as file:
            saved_lines = []
            for line in file:
                saved_lines.append(line)
        if item not in template_lines:
            properties_lines.remove(item)
    print(saved_lines)
    cfg_file = open(file_name_2, "w")
    cfg_file.truncate()
    for line in saved_lines:
        cfg_file.write(line)'''


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
        time.sleep(0.1)
# check_template()
try:
    default_file = open(file_name, "r")
except FileNotFoundError:
    tkinter.messagebox.showerror("Missing file!", "Could not find config.properties.template, please ensure that"
                                                  " it is next to this file")
    alive = False
if alive:
    root = tk.Tk()
    root.resizable(width=False, height=False)
    root.title("Pokemon GO Bot Configuration Tool")
    frameLogin = LabelFrame(root, text="Login")
    frameLogin.grid()
    frameLocation = LabelFrame(root, text="Location")
    frameLocation.grid()
    frameItems = LabelFrame(root, text="Items")
    frameItems.grid(rowspan=5)
    frameTransfer = LabelFrame(root, text="Transfer")
    frameTransfer.grid(column=1, rowspan=2, row=0, columnspan=2)
    frameLog = LabelFrame(root, text="Logging")
    frameLog.grid(column=1, row=2, rowspan=1, columnspan=2)
    frameGui = LabelFrame(root, text="Web GUI")
    frameGui.grid(column=1, row=3, columnspan=2)
    menu_bar = Menu(root)
    menu_bar.add_cascade(label="About", command=display_about)
    root.config(menu=menu_bar)
    loop = threading.Thread(target=thread_loop)
    loop.daemon = True
    loop.start()
    mainloop()
    alive = False
