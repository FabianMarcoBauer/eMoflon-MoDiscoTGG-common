package common.delta;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Monitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.compare.Comparison;
import org.eclipse.emf.compare.Diff;
import org.eclipse.emf.compare.EMFCompare;
import org.eclipse.emf.compare.merge.BatchMerger;
import org.eclipse.emf.compare.merge.IBatchMerger;
import org.eclipse.emf.compare.merge.IMerger;
import org.eclipse.emf.compare.scope.DefaultComparisonScope;
import org.eclipse.emf.compare.scope.IComparisonScope;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.gmt.modisco.java.emf.JavaPackage;
import org.eclipse.uml2.uml.UMLPackage;

//based on: https://www.eclipse.org/emf/compare/documentation/latest/FAQ.html#How_can_I_use_EMF_Compare_programmatically_.3F
public class ComaratorDelta {

	static {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());
	}
	
	private static void prepare(ResourceSet rs) {
		rs.getPackageRegistry().put("platform:/plugin/org.eclipse.gmt.modisco.java/model/java.ecore",
				JavaPackage.eINSTANCE);
		rs.getPackageRegistry().put("platform:/plugin/org.eclipse.uml2.uml/model/UML.ecore", UMLPackage.eINSTANCE);
		rs.getPackageRegistry().put(JavaPackage.eNS_URI, JavaPackage.eINSTANCE);
		rs.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);

	}

	public static void main(String[] args) {
		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

		URI uri1 = URI.createFileURI(new File("instances/src_processedA.xmi").getAbsolutePath());
		URI uri2 = URI.createFileURI(new File("instances/src_processedB.xmi").getAbsolutePath());

		Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("xmi", new XMIResourceFactoryImpl());

		ResourceSet resourceSet1 = new ResourceSetImpl();
		prepare(resourceSet1);
		ResourceSet resourceSet2 = new ResourceSetImpl();
		prepare(resourceSet2);

		Resource resource1 = resourceSet1.getResource(uri1, true);
		Resource resource2 = resourceSet2.getResource(uri2, true);

		IComparisonScope scope = new DefaultComparisonScope(resource1, resource2, null);
		Comparison comparison = EMFCompare.builder().build().compare(scope);

		List<Diff> differences = comparison.getDifferences();
		// Let's merge every single diff
		System.out.println(differences);
		IMerger.Registry mergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
		IBatchMerger merger = new BatchMerger(mergerRegistry);
		merger.copyAllLeftToRight(differences, new BasicMonitor());

		resourceSet2.getResources().forEach(r -> {
			try {
				r.save(null);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public int copyToRight(Notifier r1, Notifier r2) {
		IComparisonScope scope = new DefaultComparisonScope(r1, r2, null);
		Comparison comparison = EMFCompare.builder().build().compare(scope);

		List<Diff> differences = comparison.getDifferences();
		int size = differences.size();
		// Let's merge every single diff
		IMerger.Registry mergerRegistry = IMerger.RegistryImpl.createStandaloneInstance();
		IBatchMerger merger = new BatchMerger(mergerRegistry);
		merger.copyAllLeftToRight(differences, new BasicMonitor());
		
		return size;
	}

	public int loadFromFile(Notifier target, File source) {
		URI uri = URI.createFileURI(source.getAbsolutePath());
		ResourceSet rs = new ResourceSetImpl();
		Resource resource1 = rs.getResource(uri, true);
		prepare(rs);

		return copyToRight(resource1, target);
	}

	public int loadFromFile(Notifier target, String source) {
		System.out.println("a");
		File f = new File(source);
		if (!f.exists()) {
			throw new RuntimeException(new FileNotFoundException("File "+source+" does not exist"));
		}
		return loadFromFile(target, f);
	}
}
