package org.clulab.reach.grounding

/**
  * Trait for defining constants used by grounding and entity checking code.
  *   Written by Tom Hicks. 10/22/2015.
  *   Last Modified: Update for HMS drug KB.
  */
object ReachKBConstants {

  /** The default namespace string for KBs. */
  val DefaultNamespace: String = "uaz"

  /** The string used to separate a namespace and an ID. */
  val NamespaceIdSeparator: String = ":"


  /** The set of words to remove from all keys to create a lookup key. */
  val AllKeysStopSuffixes = Seq("_human")

  /** The set of words to remove from a key to create a protein family lookup key. */
  val FamilyStopSuffixes = Seq(" protein family", " family")

  /** The set of characters to remove from the text to create a lookup key. */
  val KeyCharactersToRemove = " /-".toSet

  /** The set of words to remove from a key to create a protein lookup key. */
  val ProteinStopSuffixes = Seq(" mutant protein", " protein")


  /** File Path to the directory which holds the entity knowledge bases. */
  val KBDirFilePath = "src/main/resources/org/clulab/reach/kb"

  /** Resource Path to the directory which holds the entity knowledge bases. */
  val KBDirResourcePath = "/org/clulab/reach/kb"


  /** Filename of the cellular location file generated by the entity checker. */
  val GendCellLocationFilename = "biopax-cellular_component.tsv.gz"
  /** Prefix string for cellular location IDs generated by the entity checker. */
  val GendCellLocationPrefix = "UA-BP-CC-"

  /** Filename of the small molecule file generated by the entity checker. */
  val GendChemicalFilename = "biopax-simple_chemical.tsv.gz"
  /** Prefix string for small molecule IDs generated by the entity checker. */
  val GendChemicalPrefix = "UA-BP-SC-"

  /** Filename of the protein/family file generated by the entity checker. */
  val GendProteinFilename = "biopax-gene_or_gene_product.tsv.gz"
  /** Prefix string for protein/family IDs generated by the entity checker. */
  val GendProteinPrefix = "UA-BP-GGP-"


  /** Filename of the static bio processes file. */
  val StaticBioProcessFilename = "bio_process.tsv.gz"

  /** Filename of the static cellular location file. */
  val StaticCellLocationFilename = "GO-subcellular-locations.tsv.gz"

  /** Filename of the alternate static cellular location file. */
  val StaticCellLocation2Filename = "uniprot-subcellular-locations.tsv.gz"

  /** Filename of the static small molecule (chemical) file. */
  val StaticChemicalFilename = "PubChem.tsv.gz"

  /** Filename of the static small molecule (drug) file. */
  val StaticDrugFilename = "hms-drugs.tsv.gz"

  /** Filename of the static small molecule (metabolite) file. */
  val StaticMetaboliteFilename = "hmdb.tsv.gz"

  /** Filename of the static protein file. */
  val StaticProteinFilename = "uniprot-proteins.tsv.gz"

  /** Filename of the static protein family file. */
  val StaticProteinFamilyFilename = "PFAM-families.tsv.gz"

  /** Filename of the secondary static protein family file. */
  val StaticProteinFamily2Filename = "ProteinFamilies.tsv.gz"


  /** Filename of the context species file */
  val ContextSpeciesFilename = "Species.tsv.gz"

  /** Filename of the context cell lines file */
  val ContextCellLineFilename = "Cellosaurus.tsv.gz"

  /** Filename of the secondary context cell lines file */
  val ContextCellLine2Filename = "atcc.tsv.gz"

  /** Filename of the context cell types file */
  val ContextCellTypeFilename = "CellOntology.tsv.gz"

  /** Filename of the context organs file */
  val ContextOrganFilename = "Uberon.tsv.gz"

  /** Filename of the static tissue type file. */
  val ContextTissueTypeFilename = "tissue-type.tsv.gz"


  /** Filename of the protein kinases lookup table. */
  val ProteinKinasesFilename = "uniprot-kinases.txt.gz"

  /** Filename of a file containing just Protein Domain suffixes; one per line. */
  val ProteinDomainShortNamesFilename = "proteinDomains-short.txt.gz"

}
