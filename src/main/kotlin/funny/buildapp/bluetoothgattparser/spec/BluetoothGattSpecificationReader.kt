package funny.buildapp.bluetoothgattparser.spec

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import org.gradle.internal.impldep.com.google.common.reflect.TypeToken
import org.gradle.internal.impldep.com.google.gson.Gson
import org.gradle.internal.impldep.com.google.gson.stream.JsonReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Type
import java.net.MalformedURLException
import java.net.URL
import java.util.*


/**
 * Bluetooth GATT specification reader. Capable of reading Bluetooth SIG GATT specifications for
 * [services and characteristics](https://www.bluetooth.com/specifications/gatt).
 * Stateful but threadsafe.
 *
 * @author Vlad Kolotov
 */
class BluetoothGattSpecificationReader() {
    /**
     * Creates an instance of GATT specification reader and pre-cache GATT specification files from java classpath
     * by the following paths: gatt/characteristic and gatt/service.
     */
    //    private val logger = LoggerFactory.getLogger(BluetoothGattSpecificationReader::class.java)
    private val servicesRegistry: MutableMap<String?, URL> = HashMap()
    private val characteristicsRegistry: MutableMap<String?, URL> = HashMap()
    private val characteristicsTypeRegistry: MutableMap<String, String> = HashMap()
    private val services: MutableMap<String, Service> = HashMap<String, Service>()
    private val characteristicsByUUID: MutableMap<String, Characteristic> = HashMap<String, Characteristic>()
    private val characteristicsByType: MutableMap<String, Characteristic> = HashMap<String, Characteristic>()

    init {
        val servicesResource = javaClass.classLoader.getResource(CLASSPATH_SPEC_FULL_SERVICE_FILE_NAME)
        val characteristicsResource = javaClass.classLoader.getResource(
            CLASSPATH_SPEC_FULL_CHARACTERISTIC_FILE_NAME
        )
        loadExtensionsFromCatalogResources(servicesResource, characteristicsResource)
    }

    /**
     * Returns GATT service specification by its UUID.
     *
     * @param uuid an UUID of a GATT service
     * @return GATT service specification
     */
    fun getService(uuid: String): Service? {
        if (services.containsKey(uuid)) {
            return services[uuid]
        } else if (servicesRegistry.containsKey(uuid)) {
            synchronized(services) {
                // is it still not loaded?
                if (!services.containsKey(uuid)) {
                    val service: Service? = loadService(uuid)
                    service?.let { addService(it) }
                    return service
                }
            }
        }
        return null
    }

    /**
     * Returns GATT characteristic specification by its UUID.
     *
     * @param uuid an UUID of a GATT characteristic
     * @return GATT characteristic specification
     */
    fun getCharacteristicByUUID(uuid: String): Characteristic? {
        if (characteristicsByUUID.containsKey(uuid)) {
            return characteristicsByUUID[uuid]
        } else if (characteristicsRegistry.containsKey(uuid)) {
            synchronized(characteristicsByUUID) {
                // is it still not loaded?
                if (!characteristicsByUUID.containsKey(uuid)) {
                    val characteristic: Characteristic? = loadCharacteristic(uuid)
                    characteristic?.let { addCharacteristic(it) }
                    return characteristic
                }
            }
        }
        return null
    }

    /**
     * Returns GATT characteristic specification by its type.
     *
     * @param type a type of a GATT characteristic
     * @return GATT characteristic specification
     */
    fun getCharacteristicByType(type: String): Characteristic? {
        if (characteristicsByType.containsKey(type)) {
            return characteristicsByType[type]
        } else if (characteristicsTypeRegistry.containsKey(type)) {
            synchronized(characteristicsByUUID) {
                // is it still not loaded?
                if (!characteristicsByType.containsKey(type)) {
                    val characteristic: Characteristic? = loadCharacteristic(
                        characteristicsTypeRegistry[type]
                    )
                    characteristic?.let { addCharacteristic(it) }
                    return characteristic
                }
            }
        }
        return null
    }

    val characteristics: Collection<Characteristic>
        /**
         * Returns all registered GATT characteristic specifications.
         *
         * @return all registered characteristic specifications
         */
        get() = ArrayList(characteristicsByUUID.values)

    /**
     * Returns all registered GATT service specifications.
     *
     * @return all registered GATT service specifications
     */
    fun getServices(): Collection<Service> {
        return ArrayList(services.values)
    }

    /**
     * Returns a list of field specifications for a given characteristic.
     * Note that field references are taken into account. Referencing fields are not returned,
     * referenced fields returned instead (see [Field.getReference]).
     *
     * @param characteristic a GATT characteristic specification object
     * @return a list of field specifications for a given characteristic
     */
    fun getFields(characteristic: Characteristic?): List<Field> {
        val fields: MutableList<Field> = ArrayList<Field>()
        if (characteristic?.value == null) {
            return emptyList<Field>()
        }
        for (field: Field in characteristic.value.fields!!) {
            if (field.reference == null) {
                fields.add(field)
            } else {
                //TODO prevent recursion loops
                fields.addAll(getFields(getCharacteristicByType(field.reference.trim())))
            }
        }
        return Collections.unmodifiableList(fields)
    }

    /**
     * This method is used to load/register custom services and characteristics
     * (defined in GATT XML specification files,
     * see an example [here](https://www.bluetooth.com/api/gatt/XmlFile?xmlFileName=org.bluetooth.characteristic.battery_level.xml))
     * from a folder. The folder must contain two sub-folders for services and characteristics respectively:
     * "path"/service and "path"/characteristic. It is also possible to override existing services and characteristics
     * by matching UUIDs of services and characteristics in the loaded files.
     * @param path a root path to a folder containing definitions for custom services and characteristics
     */
    fun loadExtensionsFromFolder(path: String) {
//        logger.info("Reading services and characteristics from folder: $path")
        val servicesFolderName = path + File.separator + SPEC_SERVICES_FOLDER_NAME
        val characteristicsFolderName = path + File.separator + SPEC_CHARACTERISTICS_FOLDER_NAME
//        logger.info("Reading services from folder: $servicesFolderName")
        readServices(getFilesFromFolder(servicesFolderName))
//        logger.info("Reading characteristics from folder: $characteristicsFolderName")
        readCharacteristics(getFilesFromFolder(characteristicsFolderName))
    }

    private fun catalogToURLs(serviceRegistry: URL, xmlEntry: Map<String, String>): Map<String?, URL> {
        val processed: MutableMap<String?, URL> = HashMap()
        for (entry: Map.Entry<String, String> in xmlEntry.entries) {
            try {
                val specUrl = getSpecResourceURL(serviceRegistry, entry.value)
//                logger.debug("Loaded {} underneath {}", entry.value, specUrl)
                processed[entry.key] = specUrl
            } catch (err: MalformedURLException) {
//                logger.error("Failed to make GATT registry entry for {} underneath {}", entry.value, serviceRegistry)
            }
        }
        return processed
    }

    fun loadExtensionsFromCatalogResources(servicesResource: URL?, characteristicsResource: URL?) {
        try {
            val loadedServices = readRegistryFromCatalogResource(servicesResource)
//        logger.info("Loaded {} GATT specifications from resource {}", loadedServices.size, servicesResource)
            val loadedServicesRegistry = servicesResource?.let { catalogToURLs(it, loadedServices) }
            val loadedCharacteristics = readRegistryFromCatalogResource(characteristicsResource)
//        logger.info("Loaded {} GATT specifications from resource {}", loadedCharacteristics.size, characteristicsResource)
            val loadedCharacteristicsRegistry = characteristicsResource?.let { catalogToURLs(it, loadedCharacteristics) }
            val loadedTypeRegistry: Map<String, String> =
                loadedCharacteristics.entries.associateBy({ it.value }, { it.key })
            if (loadedServicesRegistry != null) {
                servicesRegistry.putAll(loadedServicesRegistry)
            }
            if (loadedCharacteristicsRegistry != null) {
                characteristicsRegistry.putAll(loadedCharacteristicsRegistry)
            }
            characteristicsTypeRegistry.putAll(loadedTypeRegistry)
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun getRequirements(fields: List<Field>, flags: Field?): Set<String> {
        val result: MutableSet<String> = HashSet()
        val iterator: Iterator<Field> = fields.iterator()
        while (iterator.hasNext()) {
            val field: Field = iterator.next()
            if (field.bitField != null) {
                continue
            }
            val requirements: List<String>? = field.requirements
            if (requirements.isNullOrEmpty()) {
                continue
            }
            if (requirements.contains(MANDATORY_FLAG)) {
                continue
            }
            if (requirements.size == 1 && requirements.contains(OPTIONAL_FLAG) && !iterator.hasNext()) {
                continue
            }
            result.addAll(requirements)
        }
        return result
    }

    private fun addCharacteristic(characteristic: Characteristic) {
        validate(characteristic)
        characteristicsByUUID[characteristic.uuid ?: ""] = characteristic
        characteristicsByType[characteristic.type?.trim() ?: ""] = characteristic
    }

    private fun addService(service: Service) {
        services[service.uuid ?: ""] = service
    }

    private fun validate(characteristic: Characteristic) {
        val fields: List<Field> = characteristic.value?.fields!!
        if (fields.isEmpty()) {
//            logger.warn(
//                "Characteristic \"{}\" does not have any Fields tags, "
//                        + "therefore reading this characteristic will not be possible.", characteristic.name
//            )
            return
        }
        var flags: Field? = null
        var opCodes: Field? = null
        for (field: Field in fields) {
            if (FlagUtils.isFlagsField(field)) {
                flags = field
            }
            if (FlagUtils.isOpCodesField(field)) {
                opCodes = field
            }
        }
        val readFlags = if (flags != null) FlagUtils.getAllFlags(flags) else emptySet<String>()
        val writeFlags = if (opCodes != null) FlagUtils.getAllOpCodes(opCodes) else emptySet<String>()
        val requirements = getRequirements(fields, flags)
        val unfulfilledReadRequirements: MutableSet<String> = HashSet(requirements)
        unfulfilledReadRequirements.removeAll(readFlags)
        val unfulfilledWriteRequirements: MutableSet<String> = HashSet(requirements)
        unfulfilledWriteRequirements.removeAll(writeFlags)
        if (unfulfilledReadRequirements.isEmpty()) {
            characteristic.isValidForRead = true
        }
        if (unfulfilledWriteRequirements.isEmpty()) {
            characteristic.isValidForWrite = true
        }
        if (unfulfilledReadRequirements.isNotEmpty() && unfulfilledWriteRequirements.isNotEmpty()) {
//            logger.warn(
//                ("Characteristic \"{}\" is not valid neither for read nor for write operation "
//                        + "due to unfulfilled requirements: read ({}) write ({})."),
//                characteristic.name, unfulfilledReadRequirements, unfulfilledWriteRequirements
//            )
        }
    }

    private fun getFilesFromFolder(folder: String): List<URL> {
        val folderFile = File(folder)
        val files = folderFile.listFiles()
        if (!folderFile.exists() || !folderFile.isDirectory() || (files == null) || (files.size == 0)) {
            return emptyList()
        }
        val urls: MutableList<URL> = ArrayList()
        try {
            for (file: File in files) {
                urls.add(file.toURI().toURL())
            }
        } catch (e: MalformedURLException) {
            throw IllegalStateException(e)
        }
        return urls
    }

    private fun loadService(uuid: String): Service? {
        val url = servicesRegistry[uuid]
        return getService(url)
    }

    private fun loadCharacteristic(uuid: String?): Characteristic? {
        val url = characteristicsRegistry[uuid]
        return getCharacteristic(url)
    }

    private fun readServices(files: List<URL>) {
        for (file: URL in files) {
            val service: Service? = getService(file)
            if (service != null) {
                addService(service)
            }
        }
    }

    private fun readCharacteristics(files: List<URL>) {
        for (file: URL in files) {
            val characteristic: Characteristic? = getCharacteristic(file)
            if (characteristic != null) {
                addCharacteristic(characteristic)
            }
        }
    }

    private fun getService(file: URL?): Service? {
        return getSpec(file)
    }

    private fun getCharacteristic(file: URL?): Characteristic? {
        return getSpec(file)
    }

    private fun <T> getSpec(file: URL?): T? {
        try {
            val xstream = XStream(DomDriver())
            xstream.autodetectAnnotations(true)
            xstream.processAnnotations(Bit::class.java)
            xstream.processAnnotations(BitField::class.java)
            xstream.processAnnotations(Characteristic::class.java)
            xstream.processAnnotations(Enumeration::class.java)
            xstream.processAnnotations(Enumerations::class.java)
            xstream.processAnnotations(Field::class.java)
            xstream.processAnnotations(InformativeText::class.java)
            xstream.processAnnotations(Service::class.java)
            xstream.processAnnotations(Value::class.java)
            xstream.processAnnotations(Reserved::class.java)
            xstream.processAnnotations(Examples::class.java)
            xstream.processAnnotations(CharacteristicAccess::class.java)
            xstream.processAnnotations(Characteristics::class.java)
            xstream.processAnnotations(Properties::class.java)
            xstream.ignoreUnknownElements()
            xstream.classLoader = Characteristic::class.java.getClassLoader()
            return xstream.fromXML(file) as T
        } catch (e: Exception) {
//            logger.error("Could not read file: $file", e)
        }
        return null
    }

    private fun readRegistryFromCatalogResource(serviceRegistry: URL?): Map<String, String> {
//        logger.info("Reading GATT registry from: {}", serviceRegistry)
        if (serviceRegistry == null) {
            throw IllegalStateException("GATT spec registry file is missing")
        }
        val type: Type = object : TypeToken<Map<String?, String?>?>() {}.type
        val gson: Gson = Gson()
        var jsonReader: JsonReader? = null
        try {
            jsonReader = JsonReader(InputStreamReader(serviceRegistry.openStream(), "UTF-8"))
            return gson.fromJson<Map<String, String>>(jsonReader, type)
        } catch (e: IOException) {
            throw IllegalStateException(e)
        } finally {
            if (jsonReader != null) {
                try {
                    jsonReader.close()
                } catch (e: IOException) {
//                    logger.error("Could not close stream", e)
                }
            }
        }
    }

    companion object {
        private val MANDATORY_FLAG = "Mandatory"
        private val OPTIONAL_FLAG = "Optional"
        private val SPEC_ROOT_FOLDER_NAME = "gatt"
        private val SPEC_SERVICES_FOLDER_NAME = "service"
        private val SPEC_CHARACTERISTICS_FOLDER_NAME = "characteristic"
        private val SPEC_REGISTRY_FILE_NAME = "gatt_spec_registry.json"
        private val CLASSPATH_SPEC_FULL_SERVICES_FOLDER_NAME = (SPEC_ROOT_FOLDER_NAME + "/"
                + SPEC_SERVICES_FOLDER_NAME)
        private val CLASSPATH_SPEC_FULL_CHARACTERISTICS_FOLDER_NAME = (SPEC_ROOT_FOLDER_NAME + "/"
                + SPEC_CHARACTERISTICS_FOLDER_NAME)
        private val CLASSPATH_SPEC_FULL_CHARACTERISTIC_FILE_NAME =
            SPEC_ROOT_FOLDER_NAME + "/" + SPEC_CHARACTERISTICS_FOLDER_NAME + "/" + SPEC_REGISTRY_FILE_NAME
        private val CLASSPATH_SPEC_FULL_SERVICE_FILE_NAME =
            SPEC_ROOT_FOLDER_NAME + "/" + SPEC_SERVICES_FOLDER_NAME + "/" + SPEC_REGISTRY_FILE_NAME

        @Throws(MalformedURLException::class)
        private fun getSpecResourceURL(catalogURL: URL, characteristicType: String): URL {
            val catalogFilePath = catalogURL.file
            val lastSlashPos = catalogFilePath.lastIndexOf('/')
            var specFilePath = catalogFilePath
            if (lastSlashPos >= 0) {
                specFilePath = catalogFilePath.substring(0, lastSlashPos)
            }
            specFilePath = "$specFilePath/$characteristicType.xml"
            return URL(
                catalogURL.protocol,
                catalogURL.host,
                catalogURL.port,
                specFilePath
            )
        }
    }
}
