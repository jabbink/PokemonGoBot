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
done = False
tf = ["true", "false"]
cRow = 0
cCol = 0
firstRun = True
has_reset = False
alive = True


class Entry:
    def __init__(self, row, col, text, value):
        self.text = text
        self.row = row
        self.col = col
        self.val = value
        self.label = tk.Label(root, text=self.text)
        self.label.grid(row=self.row, column=self.col)
        if value not in tf:
            self.entry = tk.Entry(root)
            self.entry.grid(row=self.row, column=(self.col + 1))
            self.entry.insert(END, self.val)
        if value in tf:
            self.var = tk.StringVar(root)
            self.var.set(value)
            self.entry = OptionMenu(root, self.var, *tf)
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
            tkinter.messagebox.showinfo("Created file", "Created config.properties from defaults, you may now edit the "
                                                        "values, when you are done editing press the \"save\" button"
                                                        "\n You can come back and change settings at any time")
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
                    if cRow > (lines / 2):
                        cCol = 3
                        cRow = 0
                    cRow += 1
                    val.pop(-1)
                    globals()["".join(name)] = Entry(cRow, cCol, "".join(name), "".join(val))
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
        globals()["".join(name)] = Entry(cRow, cCol, "".join(name), "".join(val))
        entries.append("".join(name))
        del name[:]
        del val[:]
        save_button = tk.Button(root, text="Save", command=save)
        save_button.grid(row=(cRow + 1), column=cCol)
        reset_button = tk.Button(root, text="Reset", command=lambda: reset(True))
        reset_button.grid(row=(cRow + 1), column=(cCol + 1))


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
    menu_bar = Menu(root)
    menu_bar.add_cascade(label="About", command=display_about)
    root.config(menu=menu_bar)
    loop = threading.Thread(target=thread_loop)
    loop.daemon = True
    loop.start()
    mainloop()
    alive = False
