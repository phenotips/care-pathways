#!/usr/bin/env python
import xlrd
import argparse

# Used for generating term IDs. Numbers 1, 2, and 3 are taken by the "Test", "Care" and "Family Care" terms.
COUNT = 4
TESTS_MAP = {}


def main(args):
    """
    Gets command line arguments, and starts Excel processing.

    :param args: the command line arguments
    """
    inputfile = args.inputfile
    # If user does not provide output file name, use a default one.
    outputfile = "CarePathwaysTestsAndCare.tsv" if not args.outputfile else args.outputfile
    # Process the file data, and save the result.
    process_data(inputfile, outputfile)


def process_data(inputfile, outputfile):
    """
    Open the Excel file, read the data, and convert to a TSV format.

    :param inputfile:  the name of the Excel file that contains the data
    :param outputfile: the name of the TSV file to which processed data will be written
    """
    global TESTS_MAP

    # Open Excel file.
    book = xlrd.open_workbook(inputfile)

    # The Module A sheet.
    mod_a_sheet = book.sheet_by_index(1)
    # Process Module A data.
    format_sheet(mod_a_sheet, 2, mod_a_sheet.nrows, 4, outputfile, "Test [CP:1]")

    # Clear the map.
    TESTS_MAP = {}
    # The Module B sheet.
    mod_b_sheet = book.sheet_by_index(2)
    # Process Module B data.
    format_sheet(mod_b_sheet, 3, mod_b_sheet.nrows, 3, outputfile, "Care [CP:2]")

    # Clear the map.
    TESTS_MAP = {}
    # The Module C sheet.
    mod_c_sheet = book.sheet_by_index(3)
    # Process Module B data.
    format_sheet(mod_c_sheet, 1, 4, 1, outputfile, "Family Care [CP:3]")


def format_sheet(sheet, start_pos, num_rows, num_cols, file_name, root):
    """
    Read the sheet data and write it in correct format to the output file.

    :param sheet:     the Excel sheet that is being processed
    :param start_pos: the line number from which to start reading data
    :param num_rows:  the total number of data rows
    :param num_cols:  the total number of data columns
    :param file_name: the name of the output file
    :param root:      the root term
    """
    f = open(file_name, 'a')

    # Write the root node on its own line.
    f.write(root + '\n')
    # Process data one row at a time.
    prev_row = ["ERROR", "ERROR"]
    for i in range(start_pos, num_rows):
        row = sheet.row_values(i)
        line = root
        cur_pos = 0
        # First and second rows may have blank cells. A blank cell implies that the label from the last filled row
        # (for the same cell) applies.
        if row[0].strip() == "":
            line = line + '\t' + _attach_id(prev_row[0].strip(), True)
            cur_pos += 1
            if row[1].strip() == "":
                line = line + '\t' + _attach_id(prev_row[1].strip(), row[2].strip() != "" and 2 < num_cols - 1)
                cur_pos += 1
            else:
                prev_row[1] = row[1]
        else:
            prev_row[0] = row[0]
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
    garbage = ["other", "other:", "date data entered"]
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


def _attach_id(item, store_item):
    """
    Generates and attaches an identifier to the item. Stores this identifier iff store_item is set to true.

    :param item:       the item that needs an identifier attached to it
    :param store_item: true iff the identifier and item pair should be stored for future retrieval
    :return:           the item with the attached identifier
    """
    global TESTS_MAP
    global COUNT

    # If the ID was assigned to this item already, get it.
    if item in TESTS_MAP:
        test_id = TESTS_MAP.get(item)
    # Otherwise, update count, generate ID, and if this ID should be stored, store it in map.
    else:
        old_count = COUNT
        COUNT += 1
        test_id = "CP:" + str(old_count)
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
