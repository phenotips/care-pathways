#!/usr/bin/env python
import xlrd
import argparse

COUNT = 1
TESTS_MAP = {}
PREFIX = ""
PREFIX_STUB = ""
SUBCOUNT = 1


def main(args):
    """
    Gets command line arguments, and starts Excel processing.

    :param args: the command line arguments
    """
    inputfile = args.inputfile
    # If user does not provide output file name, use a default one.
    outputfile = "CarePathwaysTestsAndCare.tsv" if not args.outputfile else args.outputfile
    f = open(outputfile, "w")
    f.close()
    # Process the file data, and save the result.
    process_data(inputfile, outputfile)


def process_data(inputfile, outputfile):
    """
    Open the Excel file, read the data, and convert to a TSV format.

    :param inputfile:  the name of the Excel file that contains the data
    :param outputfile: the name of the TSV file to which processed data will be written
    """
    global TESTS_MAP
    global PREFIX_STUB
    global COUNT
    global SUBCOUNT

    # Open Excel file.
    book = xlrd.open_workbook(inputfile)

    # Update the prefix stub
    PREFIX_STUB = "CP:1"
    # The Module A sheet.
    mod_a_sheet = book.sheet_by_index(1)
    # Process Module A data.
    format_sheet(mod_a_sheet, 4, mod_a_sheet.nrows, 2, outputfile, "Eligibility [CP:01]", True)

    # Clear the map.
    TESTS_MAP = {}
    # Reset the count
    COUNT = 1
    # Reset important categories count
    SUBCOUNT = 1
    # Update the prefix stub
    PREFIX_STUB = "CP:2"
    # The Module B sheet.
    mod_b_sheet = book.sheet_by_index(2)
    # Process Module B data.
    format_sheet(mod_b_sheet, 2, mod_b_sheet.nrows, 5, outputfile, "Tests [CP:02]", True)

    # Clear the map.
    TESTS_MAP = {}
    # Reset the count
    COUNT = 1
    # Reset important categories count
    SUBCOUNT = 1
    # Update the prefix
    PREFIX_STUB = "CP:3"
    # The Module C sheet.
    mod_c_sheet = book.sheet_by_index(3)
    # Process Module B data.
    format_sheet(mod_c_sheet, 2, mod_c_sheet.nrows, 2, outputfile, "Care [CP:03]", True)

def get_standard_prefix():
    global PREFIX
    return PREFIX

def get_standard_count():
    global COUNT
    return COUNT

def increment_standard_count():
    global COUNT
    COUNT += 1

def get_test_prefix():
    global PREFIX
    global PREFIX_STUB
    global SUBCOUNT
    global COUNT
    PREFIX = PREFIX_STUB + str(SUBCOUNT - 1)
    COUNT = 1
    return PREFIX_STUB

def get_test_count():
    global SUBCOUNT
    return SUBCOUNT

def increment_test_count():
    global SUBCOUNT
    SUBCOUNT += 1

def format_sheet(sheet, start_pos, num_rows, num_cols, file_name, root, has_subcategories=False):
    """
    Read the sheet data and write it in correct format to the output file.

    :param sheet:              the Excel sheet that is being processed
    :param start_pos:          the line number from which to start reading data
    :param num_rows:           the total number of data rows
    :param num_cols:           the total number of data columns
    :param file_name:          the name of the output file
    :param root:               the root term
    :param has_subcategories : a subcategories marker
    """
    f = open(file_name, 'a')

    # Write the root node on its own line.
    f.write(root + '\n')
    # Process data one row at a time.
    prev_row = ["ERROR", "ERROR", "ERROR"]
    for i in range(start_pos, num_rows):
        row = sheet.row_values(i)
        line = root
        cur_pos = 0
        # First and second rows may have blank cells. A blank cell implies that the label from the last filled row
        # (for the same cell) applies.
        if row[0].strip() == "":
            line = line + '\t' + _attach_id(prev_row[0].strip(), True)
            cur_pos += 1
            if row[1].strip() == "" and 2 < num_cols:
                line = line + '\t' + _attach_id(prev_row[1].strip(), row[2].strip() != "" and 2 < num_cols - 1)
                cur_pos += 1
                if 2 < num_cols and row[2].strip() == "":
                    line = line + '\t' + _attach_id(prev_row[2].strip(), row[3].strip() != "" and 3 < num_cols - 1)
                    cur_pos += 1
                else:
                    prev_row[2] = row[2]
            else:
                prev_row[1] = row[1]
        else:
            prev_row[0] = row[0]
            _attach_id(prev_row[0].strip(), True, has_subcategories)
        # Process the data row
        process_row(f, line, row, cur_pos, num_cols)
    f.close()

def process_row(f, data, row, cur_pos, ncols):
    """
    Processes the data row.

    :param f:       the open file
    :param data:    the accumulated line of data
    :param row:     the current row being processed
    :param cur_pos: the current position in row
    :param ncols:   the total number of columns
    """
    # Stop if we've no longer looking at relevant columns.
    if cur_pos < ncols:
        cell_raw = row[cur_pos].strip()
        # If the cell is empty, we're done.
        if cell_raw != "":
            cell = _split_items(cell_raw)
            process_cell(f, data, row, cell, cur_pos, ncols)


def process_cell(f, data, row, cell, cur_pos, ncols):
    """
    Processes the data cell.

    :param f:       the open file
    :param data:    the accumulated line of data
    :param row:     the current row being processed
    :param cell:    the current cell being processed
    :param cur_pos: the current position in row
    :param ncols:   the total number of columns
    """
    # Some rows have data that should be discarded.
    garbage = ["other", "other:", "date data entered", "name of test", "name of gene", "name of panel and number of genes", "none", "name of enzymes"]
    if len(row) <= cur_pos + 1:
        next_cell = ""
    else:
        next_cell = row[cur_pos + 1].strip()
    for item in cell:
        stripped_item = item.strip()
        if stripped_item.lower() not in garbage:
            has_more_data = next_cell != "" and cur_pos + 1 < ncols - 1
            line = data + '\t' + _attach_id(stripped_item, has_more_data)
            f.write(line + '\n')
            if next_cell != "":
                process_row(f, line, row, cur_pos + 1, ncols)


def _split_items(cell_str):
    """
    Some cells contain data for multiple terms. This data is separated by commas. Split the terms, and return that list.

    :param cell_str: the string stored in the Excel data cell
    :return:         the list of terms
    """
    item_list = [x.strip() for x in cell_str.split(',')]
    return [] if (len(item_list) == 0 or item_list[0] == '') else item_list


def _attach_id(item, store_item, has_subcategories=False):
    """
    Generates and attaches an identifier to the item. Stores this identifier iff store_item is set to true.

    :param item                 : the item that needs an identifier attached to it
    :param store_item           : true iff the identifier and item pair should be stored for future retrieval
    :param has_subcategories    : a subcategories marker
    :return                     : the item with the attached identifier
    """
    global TESTS_MAP

    if has_subcategories:
        get_count = get_test_count
        increment_count = increment_test_count
        get_prefix = get_test_prefix
    else:
        get_count = get_standard_count
        increment_count = increment_standard_count
        get_prefix = get_standard_prefix

    # If the ID was assigned to this item already, get it.
    if item in TESTS_MAP:
        test_id = TESTS_MAP.get(item)
    # Otherwise, update count, generate ID, and if this ID should be stored, store it in map.
    else:
        old_count = get_count()
        increment_count()
        test_id = get_prefix() + str(old_count)
        if store_item:
            TESTS_MAP[item] = test_id
    # Return the ID.
    return item + " [" + test_id + "]"


if __name__ == "__main__":
    # Allow user to provide input and output file names.
    parser = argparse.ArgumentParser()
    parser.add_argument("-i", "--inputfile", help="Excel input file")
    parser.add_argument("-o", "--outputfile", help="Output file")
    args = parser.parse_args()
    main(args)

# cat CarePathwaysTestsAndCare.tsv | awk --field-separator="\t" '{ print NF, $0 }' | sort -n -s | cut -d" " -f2- > CarePathwaysOrderedTestsAndCare.tsv
