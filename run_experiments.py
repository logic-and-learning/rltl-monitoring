#!/usr/bin/env python3

import sys
from pathlib import Path
import tempfile
import os


# TODOs: 
# * Keep more information after downloading to fill in all fields of the table.
# * Better prompting for single character replies.
# * Check whether /dev/null is unix specific.

"""
  Uses the command line arguments and user interaction to prepare the
  formula and statistics files.

  @returns: A path to the formula, and statistics file.
"""
def prepare_files(args):
  if len(args) > 2 or len(args) == 2 and (args[1] == "--help" or args[1] == "-h"):
    # User requested help or provided too many arguments. 
    print(f"Usage: ./{args[0]} [path/to/LTL/specification/file]")
    print("Make sure all LTL formulas are in separate lines.")
    print("If no file is given, the script will download and prepare them for you.")
    print("Go ahead, try again!")
    exit()
  elif len(args) == 2:
    # User provided exactly one argument and does not need help -> Spec file already prepared.
    print(f"Reading LTL formulas from {args[1]}.")
    formula_path = args[1]
    if not Path(formula_path).exists():
      dl = input("Oh, the file does not exists! Do you want me to download them? [y/n] ")
      if dl == "y":
        return prepare_files(args[:1])
  else:
    # User did not provide a spec file. Download it for them.
    print("No LTL formula file given.")
    formula_path = handle_download()

  stats_path = ask_for_path("the statistics", "statistics.csv")

  return [formula_path, stats_path]


"""
  Handles user interaction to determine the path to a file described by `desc`.
  Offers `dft` as default path.
"""
def ask_for_path(desc, dft):
  path_str = input(f"In which file should I write {desc}? [{dft}] ")
  if len(path_str.strip()) == 0:
    # User did not enter anything meaningful; fall back to default.
    path_str = dft
  path = Path(path_str)
  if path.exists():
    reply = input(f"The file {path_str} already exists. Do you want me to clear it [c], append the results [a], or quit [q]? ")
    if reply == "a":
      pass
    if reply == "c":
      open(path, "w").close # Open in truncation mode, close immediately.
    if reply == "q":
      print("Ok, thank you and have a pleasant week!")
      exit()
      
  return path_str


"""
  Handles the download and preparation of the formula file.
"""
def handle_download():
  dl = input("Do you want me to download the specs? [y/n] ")
  if dl != "y":
    print("Ok, thank you and have a pleasant week!")
    exit()

  try:
    import requests
  except ImportError:
    print("It seems like you do not have `requests` installed.")
    print("Run `pip install requests` and try again.")
    exit()

  path_str = ask_for_path("the specs", "specs.ltl")
  path = Path(path_str)
  with path.open("w") as file:

    formula_url = "https://github.com/nondeterministic/ltl3tools/raw/master/test-suite/rv_properties.txt"
    print(f"I will download formulas from {formula_url}.")
    payload = requests.get(formula_url).text.splitlines()

    for line in payload:
      if line.startswith("- "):
        # Remove the `-` marker and transform into our format.
        line = line[2:].replace("->", "=>").replace("<>", "F")
        φ = line.replace("[]", "G").replace("&&", "&").replace("||", "|")
        file.write(str(φ).strip() + "\n")

  return path_str


"""
  Builds the project if the user agrees.
"""
def handle_build():
  build_reply = input("Do you want me to build the project? [y/n] ")
  if build_reply == "y":
    os.system("ant")
    os.system("ant jar")


"""
  Determines the number of lines in a file.
"""
def file_len(fname):
  with open(fname) as f:
      for i, l in enumerate(f):
          pass
  return i + 1


# Obtain the paths to the formulas and the desired location of the output.
[formula_path, stats_path] = prepare_files(sys.argv)
  
# Ask the user whether we should build the project and do so.
handle_build()

# For progress reporting.
num_formulas = file_len(formula_path)

# Prepare the command for running the tool on a single formula.
cmd = "java -cp 'rltlmonitor.jar:lib/*' de.mpi_sws.rltlmonitor.CommandLineInterface " 
cmd += f"-s {stats_path} "
cmd += "both " 
# Count the number of formulas already processed.
i = 0 
with open(formula_path, "r") as formulas:
  for line in formulas:
    φ = line.strip() # Discard the newline character.
    # Report progress after every 10th formula.
    if i > 0 and i % 1 == 0:
      print(f"Progress: {i}/{num_formulas}.")
    # Finalize command.
    next_cmd = cmd + "'" + φ + "'" + "> /dev/null"
    os.system(next_cmd)
    i += 1

print("That's all for today! Good bye!")

