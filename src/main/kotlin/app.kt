import khttp.get
import java.io.File
import java.util.concurrent.Executors

val url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=nucleotide&amp;id=%s&amp;rettype=fasta"

fun main(args: Array<String>) {

    val range = args[0] // first argument
    val path = args[1]  // second argument, order is hardcoded

    val regex = Regex("""([a-zA-Z]+)(\d+)-([a-zA-Z]+)(\d+)""")

    if (!regex.matches(range)) {  // minimal validation
        println("Please provide both GenBank accession numbers and output folder to work with mtget")
    }

    val groupValues = regex.matchEntire(range)!!.groupValues
    val prefix = groupValues[1]
    var start = groupValues[2].toInt()
    var end = groupValues[4].toInt()
    start = Math.min(start, end)
    end = Math.max(start, end)

    File(path).mkdirs() // create requested directory if not exists

    val executor = Executors.newFixedThreadPool(10) // thread pool
    val endIdLength = end.toString().length
    for (id in start..end) {
        executor.execute {
            // pad id with zeros to make its length same as end id length
            download("$prefix${id.toString().padStart(endIdLength, '0')}", path)
        }
    }

    executor.shutdown()
}

fun download(id: String, path: String) {
    val response = get(url.format(id), timeout = 10.0)

    if (response.statusCode != 200) return

    val name = response.text.firstRow().toFileName()
    File(path, "$name.txt").writeText(response.text)
}

// extend String class with custom functions
fun String.firstRow() : String {
    return this.split('\n')[0]
}
fun String.toFileName() : String {
    return this.toLowerCase()
            .replace(Regex("""(homo|sapiens|isolate|DNA|region|sequence|D-loop|complete|partial|mt|genome|mitochondrion|mitochondrial|mitochondria)"""), "")
            .trim()
            .replace(Regex("""[^\w.-]"""), "")
}
